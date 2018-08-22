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
//   APCPad - handles the LED drum pads including different behaviour states.
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

APCKey { // Should work on linux and osx.
	var <defaultChannel, <sustainActive,<susBus, <numScenes, <scInPort, <scOutPort;
	var  <knobs,  <pads, <transport, <side, <keys, <>scenes, <>currentScene=0, midiOut, midiName;

	*new { arg defaultChannel = 0, sustainActive=false, susBus=15, numScenes=2, scInPort=nil, scOutPort=nil;
		^super
		.newCopyArgs(defaultChannel, sustainActive, susBus, numScenes, scInPort, scOutPort)
		.init()
	}

	init {
		//Connect to MIDI sources if noone has bothered to do it yet.
		Platform.case(
			\osx, {midiName = ["APC Key 25", "APC Key 25"]},
			\linux, {midiName = ["APC Key 25", "APC Key 25"]},
			\windows, {"Who knows what happens with windows...".postln} //untested
		);
		this.setMidi;
		knobs = Array.newClear(8);
		pads = Array.newClear(40); //Array.newClear(18);
		transport = Array.newClear(12); // Transport controls on the left
		side = Array.newClear(5);
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

	detectOutPort{^MIDIClient.destinations.find(
		[MIDIClient.destinations.detect({arg item;	item.name.find(midiName[1]).notNil}) ?
			MIDIClient.destinations.detect({arg item;	item.name.find("IAC Bus 1").notNil}) //only works for osx
		]
	)}

	setMidi {
		MIDIIn.connectAll;
		//Find nanoKONTROL in and out ports
		if(scInPort.isNil, {scInPort = this.detectInPort});
		if(scOutPort.isNil, {scOutPort = this.detectOutPort.postln});
		Platform.case(
			\osx, {midiOut=MIDIOut.new(scOutPort);},
			\linux, {midiOut = MIDIOut(0); 	midiOut.connect(scOutPort);}
		);
	}

	createKnob { arg index, cc, channel;
		var name = \knob ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		knobs = knobs.put(index, LKKnob(cc, name, channel, scInPort, index));

	}

	createPad { arg index, cc, channel;
		var name = \pad ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		pads = pads.put(index, APCPad(cc, name, channel, scInPort, midiOut, index));
	}

	createKeys { arg index, sustain=false, susBus=15, channel;
		var name = \keys ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		// keys = keys.put(index, LKKeys(name, channel,sustain, scInPort));
		keys = LKKeys(name, channel, sustain, susBus, scInPort);
	}

	createTransportPad { arg index, cc, channel;
		var name = \apctranspad ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		transport = transport.put(index, APCPad(cc, name, channel, scInPort, midiOut, index,
			Dictionary[\momentary -> [\greenhi, \black], \toggle -> [\greenhi, \black], \togHold -> [\greenhi, \black, \black]]
		));
	}

	createSidePad { arg index, cc, channel;
		var name = \apcsidepad ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		side = side.put(index, APCPad(cc, name, channel, scInPort, midiOut, index,
			Dictionary[\momentary -> [\greenhi, \black], \toggle -> [\greenhi, \black], \togHold -> [\greenhi, \black, \black]]
		));
	}

	createTransport { arg index, cc, channel;
		var name = \transport ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		transport = transport.put(index, LKButton(cc, name, channel, scInPort));
	}


	setup {
		//this.freeContent;
		8.do({|i|
			this.createKnob(i, 48+i);
		});

		5.do({|y|
			8.do({|x|
				this.createPad(x+(y*8),((4-y)*8)+x)
			})
		});



		this.createKeys(0, sustainActive, susBus, 1);

		// Transport buttons are marked top left to bottom right
		8.do({|i|
			this.createTransportPad(i,64+i);
		});

		this.createTransportPad(8,81);
		this.createTransportPad(9,91);
		this.createTransportPad(10,93);
		this.createTransportPad(11,98);

		5.do({|i|
			this.createSidePad(i,82+i);
		});


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
		scenes[sceneIndex]=Array.newClear(40);
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

	selectOne { arg func, padsID=(0..39), funcOff=nil, currentSelected = 0, colorOn=nil, colorOff=nil;
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
			pads[id].set(padFunc.value(id, i), funcOff ? padFunc.value(id, i), \toggle, colorOn: colorOn, colorOff: colorOff);
		});
		if (currentSelected.notNil) {pads[padsID[currentSelected]].setState(\on);};
		//		var newScene = Array.fill(maxSize, {|i| [padFunc, {}, \toggle]});
		//		if (scene!=currentScene) {this.switchScene(scene, nil!18)};

	}

	selectOneSide { arg func, padsID=(0..4), funcOff=nil, currentSelected = 0;
		var padFunc = {|id, result|
			{|vel, index|
				func.value(result);
				padsID.do({|i|
					if (i!=id) {side[i].setState(\off)};
				});
				if (funcOff.isNil) {side[id].setState(\on)};
			};
		};
		padsID.do({|id, i|
			side[id].set(padFunc.value(id, i), funcOff ? padFunc.value(id, i), \toggle);
		});
		if (currentSelected.notNil) {side[padsID[currentSelected]].setState(\on);};
		//		var newScene = Array.fill(maxSize, {|i| [padFunc, {}, \toggle]});
		//		if (scene!=currentScene) {this.switchScene(scene, nil!18)};

	}

	clearPads { arg padsID=(0..39);
		padsID.do({|id| pads[id].setNone});
	}

	reset {
		//This is in case LK loses connectivity and needs a reset.
		//Works only if scene had been switched once, or stored with .getScene
		Platform.case(
			\osx, {
				this.setInControl;
				this.resetScene;
			},
			\linux, {
				this.freeContent;
				this.setMidi;
				this.setup;
				this.resetScene;
			}
		);
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