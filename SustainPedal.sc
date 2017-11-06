SustainPedal {
	var <bus, <>gain, <>threshLow, <>threshHigh;
	var <def, <>func127, <>func0, susFunc, <>susSynth, <name, <msgId, <args, <>state=false;

	*new { arg bus, gain=5000, threshLow=(-0.5), threshHigh=0.5;
		^super
		.newCopyArgs(bus, gain, threshLow, threshHigh )
		.init()
	}


	init {
		func127 = {};
		func0 = {};
		name='susPed_'++bus;
		msgId=100+bus;
		def= OSCdef(name, {|msg|

			if (msg[2]==msgId) {
				if (msg[3]>0) {
					if (msg[3]>1) {
						if (state==true) {
							func0.value;
							state=false;
						}
					} {
						if (state==false) {
							func127.value;
							state=true;
						}
					}

				}
			};
		}, '/tr');
		def.permanent=true;
		args=[\inBus, bus, \msgId, msgId, \gain, gain, \threshLow, threshLow, \threshHigh, threshHigh];
		susFunc={
			SystemClock.sched(0.5,{ susSynth=Synth(\susPedal, args)} );
		};
		susSynth=SynthDef(\susPedal,{|inBus=15, msgId=100, gain=5000, threshLow=(-0.5), threshHigh=0.5|
			var in=In.ar(inBus,1)*gain;
			//	var del=Delay1.ar(in);
			//	var calc=Amplitude.ar(in,attack,release);
			// in.poll(1);
			var calc=LPF.ar(in, 10);
			//	var sustain=(calc<thresh);
			var sustain=(calc<threshLow)+((calc>threshHigh)*2);
			var changed=Changed.ar(sustain);
			SendTrig.ar(changed, msgId, sustain);
		}).play(args: args);
		CmdPeriod.add(susFunc);
	}

	set { arg press, unpress;
		this.func(press, unpress);
	}

	func { arg press, unpress;
		func127 = press;
		func0 = unpress;
		^this
	}

	free {
		CmdPeriod.remove(susFunc);
		susSynth.free;
		def.free;
	}
}