// LaunchKeyMapper - a class for easy usage of Novation LaunchKey Mini controller. By Tomer Baruch 2017.
//
// Features:
//    Set functions for each element of the Launchkey
//    Control pad colors
//    Pad states - Momentary / Toggle / TogHold (toggle but with option to hold the pad)
//    Use pads as menues - select one of them.
//    Store different scenes and switch between scenes
//    Keys with internal sustain and latch controls
//
// Subclasses -
//   LKDrumpad - handles the LED drum pads including different behaviour states.
//   LKKnob - handles the knobs
//   LKButton - handles the transport buttons
//   LKKeys - handles keys includes the option for internal sustain and latch mode
//   SustainPedal - A class to use a sustain pedal connected through audio in. (Works with MOTU because they don't cut DC)
//
// Also included - LaunchControlXL - a class for controlling Novation LaunchControlXL controller. Uses the same subclasses.
//
//
// Was tested on Mac OSX and Raspbian Strech with SC3.7
//
// Adapted from Adam Juraszek's Korg Nanocontroller class, which was adapted from Jonathan Siemasko Quneo class
//

LaunchKeyMapper { // Should work on linux and osx.
	var <defaultChannel, <sustainActive,<susBus, <numScenes, <scInPort, <scInPortCtrl, <scOutPort;
	var  <knobs,  <pads, <keys, <transport, <>scenes, <>currentScene=0, midiOut, midiName;

	*new { arg defaultChannel = 0, sustainActive=false, susBus=15, numScenes=2, scInPort=nil, scInPortCtrl=nil, scOutPort=nil;
		^super
		.newCopyArgs(defaultChannel, sustainActive, susBus, numScenes, scInPort, scInPortCtrl, scOutPort)
		.init()
	}

	init {
		//Connect to MIDI sources if noone has bothered to do it yet.
		Platform.case(
			\osx, {midiName = ["LK Mini MIDI", "LK Mini InControl"]},
			\linux, {midiName = ["Launchkey Mini MIDI 1", "Launchkey Mini MIDI 2"]},
			\windows, {"Who knows what happens with windows...".postln} //untested
		);
		this.setMidi;
		knobs = Array.newClear(8);
		pads = Array.newClear(18);
		transport = Array.newClear(4); // Transport controls on the left
		scenes= Array.newClear(numScenes);
		this.postInfo;
		SystemClock.sched(0.5,{this.setup});

	}

	postInfo {
		''.postln;
		('Launchkey Mini: In - ' ++ scInPort).postln;
		(Char.tab ++ 'knobs - ' ++ knobs.size).postln;
		(Char.tab ++ 'pads - ' ++ pads.size).postln;
		(Char.tab ++ 'transport - ' ++ transport.size).postln;
		''.postln;
	}

	detectInPort{^(MIDIClient.sources.detect({arg item;
		item.name.find(midiName[0]).notNil}) !? _.uid ? 0);}

	detectInPortCtrl{^(MIDIClient.sources.detect({arg item;
		item.name.find(midiName[1]).notNil}) !? _.uid ? 0);}

	detectOutPort{^MIDIClient.destinations.find(
		[MIDIClient.destinations.detect({arg item;	item.name.find(midiName[1]).notNil}) ?
			MIDIClient.destinations.detect({arg item;	item.name.find("IAC Bus 1").notNil}) //only works for osx
		]
	)}

	setMidi {
		MIDIIn.connectAll;
		//Find nanoKONTROL in and out ports
		if(scInPort.isNil, {scInPort = this.detectInPort});
		if(scInPortCtrl.isNil, {scInPortCtrl = this.detectInPortCtrl});
		if(scOutPort.isNil, {scOutPort = this.detectOutPort.postln});
		Platform.case(
			\osx, {midiOut=MIDIOut.new(scOutPort);},
			\linux, {midiOut = MIDIOut(0); 	midiOut.connect(scOutPort);}
		);
		if (midiOut.notNil) {
			midiOut.latency=0.02; //I don't remember why this is important
			midiOut.noteOn(0,12,127);
			midiOut.control(8,0,40);
		}  //set InControl active
	}

	createKnob { arg index, cc, channel;
		var name = \knob ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		knobs = knobs.put(index, LKKnob(cc, name, channel, scInPortCtrl, index));

	}

	createPad { arg index, cc, channel;
		var name = \pad ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		pads = pads.put(index, LKDrumpad(cc, name, channel, scInPortCtrl, midiOut, index));
	}

	createKeys { arg index, sustain=false, susBus=15, channel;
		var name = \keys ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		// keys = keys.put(index, LKKeys(name, channel,sustain, scInPort));
		keys = LKKeys(name, channel,sustain,susBus, scInPort);
	}

	createTransport { arg index, cc, channel;
		var name = \transport ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		transport = transport.put(index, LKButton(cc, name, channel, scInPortCtrl));
	}


	setup {
		//this.freeContent;
		8.do({|i|
			this.createKnob(i, 21+i);
		});

		this.createPad(0, 96);
		this.createPad(1, 97);
		this.createPad(2, 98);
		this.createPad(3, 99);
		this.createPad(4, 100);
		this.createPad(5, 101);
		this.createPad(6, 102);
		this.createPad(7, 103);
		this.createPad(8, 112);
		this.createPad(9, 113);
		this.createPad(10, 114);
		this.createPad(11, 115);
		this.createPad(12, 116);
		this.createPad(13, 117);
		this.createPad(14, 118);
		this.createPad(15, 119);
		this.createPad(16, 104);
		this.createPad(17, 120);


		this.createKeys(0,sustainActive,susBus);

		// Transport buttons are marked top left to bottom right
		this.createTransport(0,106);
		this.createTransport(1,107);
		this.createTransport(2,104);
		this.createTransport(3,105);

		"launchKeys has been setup".postln;
	}

	dict {
		MIDIdef.all.postSorted;
		MIDIdef.all.know = true;
		^MIDIdef.all
	}

	getPads {
		^pads.collect({|pad| pad.getArray});
	}

	setPads { arg scene;
		scene.do({|padArray, index|
			pads[index].setArray(padArray)});
	}
	setPadOffline {arg sceneIndex, padNum, pad;
		scenes[sceneIndex][padNum]=pad;
	}
	getPadOffline {arg sceneIndex, padNum;
		^scenes[sceneIndex][padNum];
	}
	setSceneOffline {arg sceneIndex, scene;
		scenes[sceneIndex]=Array.newClear(18);
		scene.size.do({|i| scenes[sceneIndex][i]=scene[i]});
	}

	resetScene {
		this.setPads(scenes[currentScene]);
	}

	setScene {arg sceneIndex, scene;
		if (scene!=nil) {this.setSceneOffline(sceneIndex, scene)};
		currentScene=sceneIndex;
		this.resetScene;
	}
	switchScene {arg sceneIndex, scene;
		if (sceneIndex!=currentScene) {
			scenes[currentScene]=this.getPads;

		};
		this.setScene(sceneIndex, scene);
	}
	storeScene {arg sceneIndex=0;
		scenes[sceneIndex]=this.getPads;
	}
	getScene {arg sceneIndex;
		^scenes[sceneIndex];
	}

	selectOne { arg func, padsID=(0..15), funcOff=nil, currentSelected = 0;
		var padFunc = {|id, result|
			{|vel, index|
				func.value(result);
				padsID.do({|i|
					if (i!=id) {pads[i].setState(\off)};
				});
				if (funcOff.isNil) {pads[id].setState(\on)};
			};
		};
		padsID.do({|id, i|
			pads[id].set(padFunc.value(id, i), funcOff ? padFunc.value(id, i), \toggle);
		});
		if (currentSelected.notNil) {pads[padsID[currentSelected]].setState(\on);};
		//		var newScene = Array.fill(maxSize, {|i| [padFunc, {}, \toggle]});
		//		if (scene!=currentScene) {this.switchScene(scene, nil!18)};

	}

	reset {
		//This is in case LK loses connectivity and needs a reset.
		//Works only if scene had been switched once, or stored with .getScene
		this.freeContent;
		this.setMidi;
		this.setup;
		this.resetScene;
	}

	panic {
		pads.do(_.panic);
		keys.panic;
	}

	freeContent {
		knobs.do(_.free);
		pads.do(_.free);
		keys.free;
	}

	free {
		this.freeContent;
		midiOut.disconnect(scOutPort);

	}

}