# smart-aircon

A project I worked on long ago. If I remember correctly, the gist of the project
was to use an Arduino + Raspberry Pi combo to imitate an air conditioner that
could be remotely controlled from a phone on the local network, with some auto-on/off stuff using a LSTM model.

An LED on the Arduino will indicate the ON/OFF status of the "air conditioner";
It would be able to actually control an air conditioner if the correct IR
signals have been programmed in (iirc). The Raspberry Pi listens to the phone,
and when the user switches the AC off, it will send a signal to the Arduino to
do so.

But there is a twist: The Raspberry Pi also ran an LSTM model which recorded the
user behaviour (creepy!) and which predicts when the user will want the AC on
again, switching the AC on automatically at that predicted time.

The code is released under an MIT license.
