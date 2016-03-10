/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <unistd.h>
#include <stdio.h>

#include <mraa.h>

#define IO_LED		25

int main(int argc, char* argv[])
{
	mraa_init();
	printf("hello mraa\n Version: %s\n Running on %s\n", mraa_get_version(), mraa_get_platform_name());

	mraa_gpio_context gpio = mraa_gpio_init(IO_LED);
	mraa_gpio_dir(gpio, MRAA_GPIO_OUT);

	while (1) {
		mraa_gpio_write(gpio, !mraa_gpio_read(gpio));
		sleep(1);
	}
	return 0;
}
