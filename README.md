# formio-android
Library which enables you to show FormIO forms in Android WebView both online and offline

Note:
I created this library for my own needs back when I was working with FormIO on Android.

Also, I saw that this person forked my project and updated the FormIO javascript library: https://github.com/emtec-de/formio-android
They included it as a submodule which is easier to maintain but I didn't do that on purpose to avoid having such a huge dependency, so I stripped it of all unnecessary stuff and added it directly.

If you don't mind having the whole FormIO js library included in your project you can try that fork, it might work for you since it's updated to use a newer version of the FormIO.

We are actually working on a native Android and iOS implementation of FormIO which will basically use native components to render FormIO forms. It should be done sometimes in 2021 but I'm not 100% sure if it's gonna be open source.
