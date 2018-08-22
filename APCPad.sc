APCPad {
	var <cc, <name, <channel, <scInPort, <midiOut, <index, defaults_;
	var <defOn, <defOff, <>funcOn, <>funcOff, <>funcHoldOn, <>funcHoldOff, <>type=\none, <>state=\off, colors,  <defaults;
	var <>colorOn=\black, <>colorOff=\black, <>colorHold=\black, <>holdSeconds=0, minHold=0.5;

	*new { arg cc, name = \ctrl, channel, scInPort, midiOut, index, defaults;
		^super
		.newCopyArgs(cc, name, channel, scInPort, midiOut, index, defaults)
		.init()
	}


	init {
//		"drumpad".postln;
//		scOutPort.postln;
		funcOn = {};
		funcOff = {};
		colors=(black: 0, greenhi: 1, greenblink: 2, redhi:3, redblink:4, yellowhi:5, yellowblink:6 );
		defaults = defaults_?(momentary: [\redhi, \black], toggle: [\greenhi, \yellowhi], togHold: [\greenhi, \yellowhi, \redhi], external: [\redhi, \black]);
		/*		} {
		if (MIDIOut.findPort("Launchkey Mini", "LK Mini InControl").notNil) {
		midiOut=MIDIOut.newByName("Launchkey Mini", "LK Mini InControl");
			} {
		"problem initialising".postln;
			}
		};*/
	//	this.reset;
		this.setColor(\black);
	}

	reset {

	}
	setMomentary {arg colorOn_, colorOff_;
		type=\momentary;
		colorOn=colorOn_?defaults[type][0];
		colorOff=colorOff_?defaults[type][1];

		defOn.free;
		defOn = MIDIdef.noteOn(name++'_On', { arg vel;
			funcOn.value(vel.linlin(0,127,0.0,1), index);
			state=\on;
			this.setColorByState;
		}, noteNum:cc, chan:channel, srcID: scInPort);
		defOn.permanent = true;
		defOff.free;
		defOff = MIDIdef.noteOff(name++'_Off', {
			funcOff.value(index);
			state=\off;
			this.setColorByState;

		}, noteNum: cc, chan: channel, srcID: scInPort);
		defOff.permanent = true;
	}

	setToggle {arg colorOn_, colorOff_;
		type=\toggle;
		colorOn=colorOn_?defaults[type][0];
		colorOff=colorOff_?defaults[type][1];

		defOn.free;
		defOn = MIDIdef.noteOn(name++'_On', { arg vel;
			if (state==\off) {state=\on} {state=\off};
			if (state==\on) {
				funcOn.value(vel.linlin(0,127,0.0,1),index);
				funcHoldOn.value(vel.linlin(0,127,0.0,1),index);
			} {
				funcOff.value(index);
			};
			this.setColorByState;


		}, noteNum:cc, chan:channel, srcID: scInPort);
		defOn.permanent = true;
		defOff.free;
		defOff = MIDIdef.noteOff(name++'_Off', {

		}, noteNum: cc, chan: channel, srcID: scInPort);
		defOff.permanent = true;

	}

	setTogHold {arg colorOn_, colorOff_, colorHold_;
		type=\togHold;
		colorOn=colorOn_?defaults[type][0];
		colorOff=colorOff_?defaults[type][1];
		colorHold=colorHold_?defaults[type][2];

		defOn.free;
		defOn = MIDIdef.noteOn(name++'_On', { arg vel;
			funcHoldOn.value(index);
			this.setColor(colorHold);
			holdSeconds=Date.getDate.rawSeconds;

		}, noteNum:cc, chan:channel, srcID: scInPort);
		defOn.permanent = true;
		defOff.free;
		defOff = MIDIdef.noteOff(name++'_Off', {
			funcHoldOff.value(index);
			if ((Date.getDate.rawSeconds - holdSeconds) < minHold) {
				if (state==\off) {state=\on} {state=\off};
				if (state==\on) {
					funcOn.value(index);
				} {
					funcOff.value(index);
				};
			};

			this.setColorByState;

		}, noteNum: cc, chan: channel, srcID: scInPort);
		defOff.permanent = true;

	}

	setNone {arg colorOn_, colorOff_;
		funcOn={};
		funcOff={};
		defOn.free;
		defOff.free;
		colorOn=colorOn_?\black;
		colorOff=colorOff_?\black;
		type=\none;
		this.setColorByState;
	}

	setExternal {arg colorOn_, colorOff_;
		type=\external;
		colorOn=colorOn_?defaults[type][0];
		colorOff=colorOff_?defaults[type][1];

		defOn.free;
		defOn = MIDIdef.noteOn(name++'_On', { arg vel;
			funcOn.value(vel.linlin(0,127,0.0,1), index, this);
			//state=\on;
			//this.setColorByState;
		}, noteNum:cc, chan:channel, srcID: scInPort);
		defOn.permanent = true;
		defOff.free;
		defOff = MIDIdef.noteOff(name++'_Off', {
			funcOff.value(index, this);
			//state=\off;
			//this.setColorByState;

		}, noteNum: cc, chan: channel, srcID: scInPort);
		defOff.permanent = true;
	}

	setColorByState {
		switch (state)
		{\on} {this.setColor(colorOn)}
		{\off} {this.setColor(colorOff)}
	}

	setColor { arg color;
		if (midiOut.notNil) {midiOut.noteOn(channel, cc, colors[color]?0)};
	}
	setArray { arg argArray;
		var array;
		if (argArray.isNil) {
			array=[nil, nil, \none, \off, nil, nil, nil, nil, nil];
		} {
			array=[{}, {}, \momentary, \off, nil, nil, nil, nil, nil];
			argArray.size.do({|i| array[i]=argArray[i]});
		};
		this.set(array[0], array[1], array[2], array[3],array[4],array[5], array[6], array[7], array[8]);
	}

	getArray {
		^[funcOn, funcOff, type, state, colorOn, colorOff, colorHold, funcHoldOn, funcHoldOff];
	}

	func { arg on, off, holdOn, holdOff;
		funcOn = on;
		funcOff = off;
		funcHoldOn = holdOn;
		funcHoldOff = holdOff;

	}

	set { arg on, off, setType=\momentary, setState=\off, colorOn, colorOff, colorHold, holdOn, holdOff;
		if ((setState=='on')||(setState=='off')) {state=setState}
		{
			if (setState==true) {state='on'} {state='off'}
		};
		this.func(on, off, holdOn, holdOff);
		this.setType(setType, colorOn, colorOff, colorHold);
		this.setColorByState;
		^this
	}

	setType { arg setType, colorOn, colorOff, colorHold;
		switch (setType)
		{\momentary} {this.setMomentary(colorOn, colorOff)}
		{nil} {this.setMomentary(colorOn, colorOff)}
		{\toggle} {this.setToggle(colorOn, colorOff)}
		{\none} {this.setNone(colorOn, colorOff)}
		{\external} {this.setExternal(colorOn, colorOff)}
		{\togHold} {this.setTogHold(colorOn, colorOff, colorHold)}
	}

	setState { arg setState='on';
		state=setState;
		this.setColorByState;
	}

	panic {
		funcHoldOff.value(index);
		funcOff.value(index);
		this.setColorByState;
	}
	free {
		defOn.free;
		defOff.free;
	}
}