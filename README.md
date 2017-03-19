# GBN-SAW-protocol--Error-Control
Error control in GBN and SAW protocols

To simulate the packet loss, we will use pseudo random number to control that.
First program will ask the user to input a number between [0, 99]. This number means the percentage of packet losses will happen in the transmission.
Then each time before you send your protocol data unit, your program will generate a pseudo random number in the range [0, 99] with the random seed set to the current system time. If the random number generated is less than the user input number, then the current protocol data unit won’t be sent to simulate a packet loss.
It also calculates time taken to transmit the data , number of packets sent .Also the number of packets resent due to packet loss.
For SAW protocol the field "Seq_Num_Mod" in packet class should be modified to 2.
