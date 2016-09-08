OCF Compliant Brillo Jukebox Demo
=====

This repository contains the source code of the Brillo 2.0 based Jukebox demo that consists of a MP3 player and its Android companion app. The MP3 player is designed for running on [Intel Edison with Arduino breakout board](https://www.arduino.cc/en/ArduinoCertified/IntelEdison) with [Grove LCD RGB backlight module](http://wiki.seeed.cc/Grove/Display/Grove_LCD_RGB_Backlight/) to display the played MP3 titles, it also integrates the [Iotivity base library for Android](https://api-docs.iotivity.org/latest-java/index.html) to be discovered and controlled by the companion app via [OCF](https://openconnectivity.org/) standards. The demo represents the jukebox device as two [standard OC resources](https://openconnectivity.org/wp-content/uploads/2016/06/OIC_1.1_Candidate_Specification.zip) of type Brightness and Audio Controls, and a vendor proprietary media player resource for controlling MP3 playback.

### Prebuilt Images
- **MP3 Player**  
  To setup the MP3 player demo on Intel Edison with Arduino breakout board, follows these [instructions](https://software.intel.com/en-us/flashing-firmware-with-flash-tool-lite) to flash the prebuilt Brillo OS image to the device before installing the MP3 player APK from the Android Studio.
    + [Prebuilt Brillo OS image](https://drive.google.com/open?id=0B8-BcoYPJr2LZ2N3am5NNGszRFk)
    + [Prebuilt Iotivity base library for Android](https://drive.google.com/open?id=0B8-BcoYPJr2LQkZzZ3lpekh0UkU)  
    <sup><i>Notes on building the library from [Iotivity](https://www.iotivity.org/) source  
        1. This [patch](https://gerrit.iotivity.org/gerrit/#/c/7595/) is required as the [Android dynamic linker expects SONAME attribute](https://developer.android.com/about/versions/marshmallow/android-6.0-changes.html#behavior-runtime) presented in shared libraries since Android 6.0  
        2. This [patch](https://gerrit.iotivity.org/gerrit/#/c/10165/) will be required if the [OcDeviceInfo](https://api-docs.iotivity.org/latest-java/index.html) object will be used to register a device.</i></sup>

    + [MP3 player APK](https://drive.google.com/open?id=0B8-BcoYPJr2LVGhiQXJEY1JORzg) (Resource servers)
    <p>
- **Android companion app**  
    The companion app requires Android devices support API 23 or above.
    + [Companion app APK](https://drive.google.com/open?id=0B8-BcoYPJr2LSTdpYllRQnpyRkk) (Resource clients)

### Build Instructions
1. Clone the [repository](https://github.com/ttzeng/oc-jukebox-b2) as a local project.
<pre>$ git clone https://github.com/ttzeng/oc-jukebox-b2.git
</pre>
2. Download and move the [prebuilt Iotivity base library for Android](https://drive.google.com/open?id=0B8-BcoYPJr2LQkZzZ3lpekh0UkU) into <code>iotivity-base-x86-release</code> folder and import the project in Android Studio.
3. The project contains two modules, select the module <code>demo</code> or <code>companion</code> in the Project panel, and then click <b>Build</b> > <b>Make Selected Modules</b> to build the module using Gradle.
4. For setting up the MP3 player demo on the Brillo device, copy some MP3 files to the <code>/sdcard/Music/</code> folder on the device manually with the following command.
<pre>$ adb push file.mp3 /sdcard/Music
</pre>
5. Click <b>Run</b> to generates a debug APK and deploys to the target device.

#### Screenshots
- Complete set  
<img src="http://ttzeng.github.io/doc/assets/OC-Jukebox-Brillo2.jpg" border="1" alt="Brillo2 Jukebox demo" />
- Three Card widgets represent the found resources in the companion app  
<img src="http://ttzeng.github.io/doc/assets/OC-Jukebox-Brillo2.png" border="1" alt="Screenshot of the Jukebox companion app" />
