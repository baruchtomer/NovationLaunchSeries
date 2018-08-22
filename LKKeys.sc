LKKeys {
	var <name, <channel, <sustainActive, <susBus, <scInPort, <ctrl;
	var <defOn, <defOff, <defSus, <>funcOn, <>funcOff, <>susPedal, <>sustain=false, <>heldKeys, <>susKeys, <>susFunc, <>mono=false, <>lastNote;
	var latch = false, latchFlag=false;
	*new { arg  name = \keys, channel, sustainActive=false, susBus, scInPort;
		^super
		.newCopyArgs(name, channel, sustainActive, susBus, scInPort)
		.init()
	}


	init {
		funcOn = {};
		funcOff = {};
		this.resetKeys;
		defOn = MIDIdef.noteOn(name++'_On', { arg vel,note;
			susKeys.size.do({|i| if (susKeys[i]==note) {susKeys.removeAt(i); funcOff.value(note)}});
			heldKeys=heldKeys.add(note).sort;
			if ((latch) && (latchFlag==false)) {
				latchFlag = true;
				this.susOff;
				this.susOn;
			};
			funcOn.value(vel.linlin(0,127, 0.0,1),note);
			lastNote=note;

		}, chan:channel, srcID: scInPort);
		defOn.permanent = true;
		defOff = MIDIdef.noteOff(name++'_Off', { arg vel, note;
			heldKeys.size.do({|i| if (heldKeys[i]==note) {heldKeys.removeAt(i)}});
			if (sustain) {
				susKeys=susKeys.add(note).sort;
			} {
				if ((mono) && (heldKeys.size>0)) {
					if (note==lastNote) {
						funcOn.value(vel.linlin(0,127, 0.0,1), heldKeys.last);
						lastNote=heldKeys.last;
					}
				} {
					funcOff.value(note);
				}
			};
			latchFlag = false;

		}, chan: channel, srcID: scInPort);
		defOff.permanent = true;
		if (sustainActive) {this.setSustain};


	}

	setSustain {
		susPedal=SustainPedal.new(susBus);
		this.setSustainDefault;
	}
	setSustainDefault {
		susPedal.func({this.susOn}, {this.susOff});
	}
	susOn {sustain=true}
	susOff {
		sustain=false;
		while ({susKeys.size>0}) {
			var note=susKeys[0];
			susKeys.removeAt(0);
			funcOff.value(note);
		};
		susKeys=[];
	}
	setLatch {|state = true|
		latch = state;
		if (latch) {this.susOn} {this.susOff}
	}
	getKeys {
		^heldKeys++susKeys;
	}

	panic {
		var keys=heldKeys++susKeys;
		keys.do({|note| funcOff.value(note)});
		this.resetKeys;
	}
	resetKeys{

		heldKeys=[];
		susKeys=[];
	}
	set {arg on, off, monophonic=false;
		this.func(on, off);
		mono=monophonic;
	}

	func { arg on, off;
		funcOn = on;
		funcOff = off;
		^this
	}

	free {
		susPedal.free;
		defOff.free;
		defOn.free;
		defSus.free;
	}

}