# Scrcpy for Android

- This application is android port to desktop applicaton [**Scrcpy**](https://github.com/Genymobile/scrcpy).

- This application mirrors display and touch controls from a remote android device to android device.

- Scrcpy for Android uses the ADB interface to connect to the android device to be mirrored, either over the network (WiFi) or over a USB cable.



## Download

[scrcpy-release.apk](https://github.com/zwc456baby/ScrcpyForAndroid/releases)


![home](home.jpg)



## Instructions to use (WiFi)

- Make sure both devices are on same local network.
- Enable **ADB-connect/ADB-wireless/ADB over network** on the device to be mirrored. 
- Open scrcpy-android app and enter ip address of device to be mirrored.
- Select display parameters and bitrate from drop-down menu(1280x720 and 2Mbps works best).
- Set **Navbar** switch if the device to be mirrored has only hardware navigation buttons.
- Hit **start** button.
- Accept and trust(check always allow from this computer) the ADB connection prompt on target device(Some custom roms don't have this prompt).
- Thats all! You should be seeing the screen of remote android device.
- To wake up the remote device, **double tap anywhere on screen**.
- To put the remote device to sleep, **close proxmity sensor and double tap anywhere on the screen**. 
- To bring back the local android system navbar while mirroring the remote device, **swipe up from the bottom edge of screen**.



## Instructions to use (USB)

- Connect the target device to this phone with a USB cable (USB-C to USB-C, or an OTG adapter).
- Enable **USB debugging** in Developer options on the target device.
- Open the app. When an ADB-capable USB device is detected, the address field is automatically pre-selected to `USB: <device>`.
  - You can also tap the address drop-down and choose the `USB:` entry manually.
- Select display parameters and bitrate from the drop-down menus, same as the WiFi flow.
- Hit **start**.
- Grant the USB permission prompt on this phone, then accept the **Allow USB debugging?** prompt on the target device (check *Always allow from this computer* to skip it next time).


## Connecting to public network devices



>  The public network port of the device needs to be open for access



### Connection Example

- 192.168.1.222

- host.example.com:5555

- [2000:2000:2000:2000::2000]:5555

## Code Reference

- [scrcpy-android](https://gitlab.com/las2mile/scrcpy-android)
- [scrcpy](https://github.com/Genymobile/scrcpy)

