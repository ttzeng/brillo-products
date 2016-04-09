#include <map>
#include <mraa.h>
#include "Arduino.h"

class Gpio {
public:
	Gpio() {};
	mraa_gpio_context Context(uint8_t pin);
private:
	std::map<int, mraa_gpio_context> map;
} gpio;

mraa_gpio_context Gpio::Context(uint8_t pin)
{
	mraa_gpio_context context;
	try {
		context = map.at(pin);
	}
	catch (std::out_of_range& oor) {
		context = mraa_gpio_init(pin);
		map.insert(std::pair<int, mraa_gpio_context>(pin, context));
	}
	return context;
}

void pinMode(uint8_t pin, uint8_t mode)
{
	mraa_gpio_dir_t dir = (mode == OUTPUT)? MRAA_GPIO_OUT : MRAA_GPIO_IN;
	mraa_gpio_dir(gpio.Context(pin), dir);
}

void digitalWrite(uint8_t pin, uint8_t val)
{
	mraa_gpio_write(gpio.Context(pin), val);
}

int digitalRead(uint8_t pin)
{
	return mraa_gpio_read(gpio.Context(pin));
}
