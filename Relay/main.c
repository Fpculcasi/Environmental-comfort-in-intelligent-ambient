/*
 * \file
 *      Networked Embedded System Project.
 * \author
 *		FPCulcasi
 */

#include <stdlib.h>
#include <string.h>

#include "contiki.h"
#include "contiki-net.h"

#include "er-coap.h"
#include "er-coap-engine.h"
#include "rest-engine.h"

#include "sys/etimer.h"				// event timer
#include "dev/leds.h"				// leds
#include "dev/light-sensor.h"		// ligth sensors
#include "dev/sht11/sht11-sensor.h"	// thermal and hygrometric sensor
/*
#if PLATFORM_HAS_BUTTON
	#include "dev/button-sensor.h"	// button
#endif
*/

#define DEBUG 1
#if DEBUG
	#include <stdio.h>				// For printf()
	#define PRINTF(...) printf(__VA_ARGS__)
	#define PRINT6ADDR(addr) PRINTF("[%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x:%02x%02x]", ((uint8_t *)addr)[0], ((uint8_t *)addr)[1], ((uint8_t *)addr)[2], ((uint8_t *)addr)[3], ((uint8_t *)addr)[4], ((uint8_t *)addr)[5], ((uint8_t *)addr)[6], ((uint8_t *)addr)[7], ((uint8_t *)addr)[8], ((uint8_t *)addr)[9], ((uint8_t *)addr)[10], ((uint8_t *)addr)[11], ((uint8_t *)addr)[12], ((uint8_t *)addr)[13], ((uint8_t *)addr)[14], ((uint8_t *)addr)[15])
	#define PRINTLLADDR(lladdr) PRINTF("[%02x:%02x:%02x:%02x:%02x:%02x]",(lladdr)->addr[0], (lladdr)->addr[1], (lladdr)->addr[2], (lladdr)->addr[3],(lladdr)->addr[4], (lladdr)->addr[5])
#else
	#define PRINTF(...)
	#define PRINT6ADDR(addr)
	#define PRINTLLADDR(addr)
	
#endif

#define MAX_PAYLOAD_LEN	192

/* FIXME: This server address is hard-coded for Cooja and link-local for unconnected border router. */
#define SERVER_NODE(ipaddr)	 uip_ip6addr(ipaddr, 0xaaaa, 0, 0, 0, 0, 0, 0, 0x0001)

#define REMOTE_PORT		 UIP_HTONS(COAP_DEFAULT_PORT)

float stof(const char* s){
	if (!s || !*s) return 0;
	float integerPart = 0;
	float fractionPart = 0;
	int divisorForFraction = 1;
	int sign = 1;
	int inFraction = 0;
	
	if (*s == '-'){
		++s;
		sign = -1;
	} else if (*s == '+') ++s;
	
	while (*s != '\0'){
		if (*s >= '0' && *s <= '9'){
			if (inFraction){
				fractionPart = fractionPart*10 + (*s - '0');
				divisorForFraction *= 10;
			}else integerPart = integerPart*10 + (*s - '0');
		}else if (*s == '.'){
			if (inFraction) return sign * (integerPart + fractionPart/divisorForFraction);
			else inFraction = 1;
		}else return sign * (integerPart + fractionPart/divisorForFraction);
		++s;
	}
	return sign * (integerPart + fractionPart/divisorForFraction);
}

/*---------------------------------------------------------------------------*/
PROCESS(InitProcess, "Init Process");
PROCESS(IdleProcess, "Idle Process");
PROCESS(ActiveProcess, "Active Process");
AUTOSTART_PROCESSES(&InitProcess);

static coap_packet_t request[1];
/* This way the packet can be treated as pointer as usual. */

static int temp_count, lum_count;
char msg[MAX_PAYLOAD_LEN];
uip_ipaddr_t server_ipaddr;
static unsigned char blinking_leds;
static int phase;

/*
 * Resources to be activated need to be imported through the extern keyword.
 * The build system automatically compiles the resources in the corresponding sub-directory.
 */
extern resource_t activate;		// resource to request activation
extern resource_t temp;		// resource to request temperature adjustaments
extern resource_t lum;		// resource to request light adjustaments

void activate_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){
	size_t len = 0;
	const char *mode = NULL;
	//uint8_t led = 0;
	int success = 1;

	if ((len=REST.get_post_variable(request, "mode", &mode))) {
		PRINTF("mode %s\n", mode);
		PRINTF("len %d\n", len);

		//if the "ON" command arrive and the sensor is idle
		if (strncmp(mode, "on", len)==0 && phase == 1) {
			PRINTF("Switch on sensor\n");
			process_start(&ActiveProcess,NULL);
			process_exit(&IdleProcess);
		}
		//if the "OFF" command arrive and the sensor is active
		else if (strncmp(mode, "off", len)==0 && phase == 2) {
			PRINTF("Switch off sensor\n");
			process_start(&IdleProcess,NULL);
			process_exit(&ActiveProcess);
		} else {
			success = 0;
		}
	} else {
		success = 0;
	}

	if (!success) {
		REST.set_response_status(response, REST.status.BAD_REQUEST);
	} else REST.set_response_status(response, REST.status.CREATED);
}

void temp_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){
	const char *temp = NULL;

	if (REST.get_post_variable(request, "temp", &temp)) {
		double temp_val = stof(temp);
		PRINTF("temp string %s\n", temp);
		temp_count = temp_val;
		double frac = temp_val - temp_count;
		PRINTF("temp val %d.%02u\n", temp_count, (unsigned int)(frac*100));
		if(temp_count>0) leds_on(LEDS_GREEN);
		else if(temp_count<0) leds_off(LEDS_GREEN);
		blinking_leds &= ~LEDS_GREEN;
		//PRINTF("temp_handler: leds blinking (%d%d%d)\n",(blinking_leds&4)>>2,(blinking_leds&2)>>1,blinking_leds&1);
		temp_count = abs(temp_count);
		temp_count = (temp_val==0) ? 1 : temp_count*2;
	}
	
	REST.set_response_status(response, REST.status.CREATED);
}

void lum_handler(void* request, void* response, uint8_t *buffer, uint16_t preferred_size, int32_t *offset){
	const char *lum = NULL;

	if (REST.get_post_variable(request, "lum", &lum)) {
		double lum_val = stof(lum);
		PRINTF("lum string %s\n", lum);
		lum_count = lum_val;
		double frac = lum_val - lum_count;
		PRINTF("lum val %d.%02u\n", lum_count, (unsigned int)(frac*100));
		if(lum_count>0) leds_on(LEDS_BLUE);
		else if(lum_count<0) leds_off(LEDS_BLUE);
		blinking_leds &= ~LEDS_BLUE;
		//PRINTF("lum_handler: leds blinking (%d%d%d)\n",(blinking_leds&4)>>2,(blinking_leds&2)>>1,blinking_leds&1);
		lum_count = abs(lum_count);
		lum_count = (lum_val==0) ? 1 : lum_count*2;
	}
	
	REST.set_response_status(response, REST.status.CREATED);
}

RESOURCE(activate,
         "title=\"Activation Resource\";rt=\"utility\"",
         NULL,
         activate_handler,
         NULL,
         NULL);
RESOURCE(temp,
         "title=\"Temperature Settings\";rt=\"temperature\"",
         NULL,
         temp_handler,
         NULL,
         NULL);
RESOURCE(lum,
         "title=\"Light Settings\";rt=\"light\"",
         NULL,
         lum_handler,
         NULL,
         NULL);
/*---------------------------------------------------------------------------*/
/* This function is will be passed to COAP_BLOCKING_REQUEST() to handle responses. */
void receiver_callback(void *response){
	PRINTF("---ret: respcode %d\n", ((coap_packet_t*)response)->code);
}

PROCESS_THREAD(InitProcess, ev, data)
{
	PROCESS_BEGIN();

	PRINTF("Process Init: start\n");

	/* Initialize the REST engine. */
	rest_init_engine();
	/* receives all CoAP messages */
	coap_init_engine();

	/* Activate the activation resources. */
	rest_activate_resource(&activate, "activate");
	rest_activate_resource(&temp, "temp");
	rest_activate_resource(&lum, "lum");
	
	/* Register this node to the gateway */
	SERVER_NODE(&server_ipaddr);
	leds_on(LEDS_ALL);
	
	phase=0;
	temp_count=0;
	lum_count=0;
	
	/* prepare request, TID is set by COAP_BLOCKING_REQUEST() */
	coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
	coap_set_header_uri_path(request, "/register");
	coap_set_payload(request, ROOM, ROOM_LEN);
	coap_set_header_content_format(request, APPLICATION_LINK_FORMAT);
	COAP_BLOCKING_REQUEST(&server_ipaddr, REMOTE_PORT, request, receiver_callback);

	leds_off(LEDS_ALL);
	process_start(&IdleProcess,NULL);

	PROCESS_END();
}/*---------------------------------------------------------------------------*/

PROCESS_THREAD(IdleProcess, ev, data)
{
	PROCESS_BEGIN();

	PRINTF("Process Idle: start\n");

	/* Red led is switched ON to notify the idle state */
	leds_on(LEDS_RED);
	leds_off(LEDS_GREEN & LEDS_BLUE);
	phase=1;

	while(1){
		PRINTF("Process Idle: loop\n");

		//IdleProcess wait until the resource handler notifies an activation event has come
		PROCESS_WAIT_EVENT();
		if(ev==PROCESS_EVENT_EXIT){
			PRINTF("Process Idle: exit\n");
		} else if (ev==PROCESS_EVENT_EXITED){
			PRINTF("Process Idle: another process exited\n");
		}
		
		//if someone notify me to switch on this process has to kill itself and start ActiveProcess
		//...
	}
	PROCESS_END();
}

/*---------------------------------------------------------------------------*/

/* This function is will be passed to COAP_BLOCKING_REQUEST() to handle responses. */
void
client_chunk_handler(void *response){
	const uint8_t *chunk;
	int len = coap_get_payload(response, &chunk);
	printf("|%.*s\n", len, (char *)chunk);
}

PROCESS_THREAD(ActiveProcess, ev, data)
{
	static struct etimer blink_timer;
	static int count = 0;

	PROCESS_BEGIN();
	PRINTF("Process Active: start\n");
	phase=2;
	blinking_leds = LEDS_ALL;
	leds_on(LEDS_ALL);
	
	etimer_set(&blink_timer, CLOCK_SECOND);
	
	while(1){
		PROCESS_WAIT_EVENT();
		if (etimer_expired(&blink_timer)){
			int leds = leds_get();
			PRINTF("Process Active: leds light (%d%d%d)\n",(leds&4)>>2,(leds&2)>>1,leds&1);
			PRINTF("Process Active: leds blinking (%d%d%d)\n",(blinking_leds&4)>>2,(blinking_leds&2)>>1,blinking_leds&1);
			//only once on a minute take new values of temp and light
			if(count++==20){
				count=1;
				SENSORS_ACTIVATE(light_sensor);
				SENSORS_ACTIVATE(sht11_sensor);
				double l = 10*light_sensor.value(LIGHT_SENSOR_PHOTOSYNTHETIC)/7;
				double t = (sht11_sensor.value(SHT11_SENSOR_TEMP)/100-396)/10;
				int t_dec = t;
				int l_dec = l;
				float t_frac = t - t_dec;
				float l_frac = l - l_dec;
				PRINTF("Process Active: light %d.%02u\n", t_dec, (unsigned int)(t_frac*100));
				PRINTF("Process Active: temp %d.%02u\n", l_dec, (unsigned int)(l_frac*100));
				SENSORS_DEACTIVATE(light_sensor);
				SENSORS_DEACTIVATE(sht11_sensor);
				
				/* prepare request, TID is set by COAP_BLOCKING_REQUEST() */
				coap_init_message(request, COAP_TYPE_CON, COAP_POST, 0);
				coap_set_header_uri_path(request, "/update");
				int n = sprintf(msg, "%d.%02u;%d.%02u;%s", t_dec, (unsigned int)(t_frac*100), l_dec, (unsigned int)(l_frac*100), ROOM);
				coap_set_payload(request, (uint8_t *)msg, n);
				COAP_BLOCKING_REQUEST(&server_ipaddr, REMOTE_PORT, request, client_chunk_handler);
			}
			if(temp_count != 0){
				//PRINTF("*temp_count %d\n", temp_count);
				
				//resynchronize green led blink
				if(--temp_count == 0){
					blinking_leds |= LEDS_GREEN;
					if((leds_get() & LEDS_RED) == LEDS_RED) leds_on(LEDS_GREEN);
					else leds_off(LEDS_GREEN);
				}
			}
			if(lum_count != 0){
				//PRINTF("*lum_count %d\n", lum_count);
		
				//resynchronize blue led blink
				if(--lum_count == 0){
					blinking_leds |= LEDS_BLUE;
					if((leds_get() & LEDS_RED) == LEDS_RED) leds_on(LEDS_BLUE);
					else leds_off(LEDS_BLUE);
				}
			}
			leds_toggle(blinking_leds);
			etimer_reset(&blink_timer);

		} else if (ev==PROCESS_EVENT_EXIT){
			PRINTF("Process Active: exit\n");
		} else if (ev==PROCESS_EVENT_EXITED){
			PRINTF("Process Active: another process exited\n");
		} else {
			PRINTF("Process Active: Event unknown\n");
		}
	}

	PROCESS_END();
}

