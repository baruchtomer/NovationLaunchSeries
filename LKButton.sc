LKButton {
	var <cc, <name, <channel, <scInPort, <ctrl;
	var <def, <>func127, <>func0, <isPressed=false, isTemp=false, temp127, temp0;

	*new { arg cc, name = \ctrl, channel, scInPort;
		^super
		.newCopyArgs(cc, name, channel, scInPort)
		.init()
	}


	init {
		func127 = {};
		func0 = {};
		def = MIDIdef.cc(name, { arg val;
			switch( val,
				127, {func127.value(); isPressed=true},
				0, {func0.value(); isPressed=false};
			);
		}, ccNum: cc, chan: channel, srcID: scInPort);
		def.permanent = true;
	}

	func { arg press, unpress;
		func127 = press;
		func0 = unpress;
		^this
	}
	set {arg press, unpress;
		this.func(press, unpress);
		^this
	}
	setTemp {arg press, unpress;
		if (isTemp==false) {
			temp127=func127;
			temp0 = func0;
		};
		this.func(press, unpress);
		isTemp = true;
	}
	unSetTemp {
		if (isTemp) {
			this.func(temp127, temp0);
		};
		isTemp = false;
	}

	free {
		def.free;
	}
}