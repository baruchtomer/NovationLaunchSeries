LKKnob {
	var <cc, <name, <channel, <scInPort, <index;
	var <ctrl, <def, <>func, <value, <>temporary=false, <>storedFunc=nil, <>storedVal, delta;

	*new { arg cc, name = \ctrl, channel, scInPort, index;
		^super
		.newCopyArgs(cc, name, channel, scInPort, index)
		.init()
	}


	init {
		func = {};
		value=0.5;
		def = MIDIdef.cc(name, { arg val; var newVal;
			newVal = val.linlin(0,127, 0.0, 1);
			delta = (newVal - value).linlin(-1.0,1, -127, 127);
			value = newVal;
			func.value(value, delta);
		}, ccNum: cc, chan: channel, srcID: scInPort);
		def.permanent = true;
	}

	set { arg funcOn, init;
		if (init!=nil) {value=init};
		func=funcOn;
	}

	setTemp { arg tempFunc, init;
		if (temporary==false) {
			storedVal = value;
			storedFunc = func;
		};
		func = tempFunc;
		if (init.notNil) {value = init};
		temporary = true;
	}

	unSetTemp {
		if (temporary) {
			value = storedVal;
			func = storedFunc;
		}
	}


}