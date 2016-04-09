#include <unistd.h>
#include "Arduino.h"

void delay(unsigned long ms)
{
	usleep(ms * 1000);
}
