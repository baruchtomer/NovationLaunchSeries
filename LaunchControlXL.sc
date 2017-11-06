// LaunchControlXL - a class for easy usage of Novation LaunchControlXL controller. By Tomer Baruch 2017.
//
// Features:
//    Set functions for each element of the Launchcontrol
//    Control pad colors
//    Pad states - Momentary / Toggle / TogHold (toggle but with option to hold the pad)
//    Use pads as menues - select one of them.
//    Store different scenes and switch between scenes
//
// Subclasses -
//   LKDrumpad - handles the LED pads including different behaviour states.
//   LKKnob - handles the knobs and faders
//   LKButton - handles the transport buttons
//
// Also included - LaunchKeyMapper - a class for controlling Novation LaunchControlXL controller. Uses the same subclasses.
//
// Was tested on Mac osx running SC3.7
//
//
// Adapted from Adam Juraszek's Korg NanoController class, which was adapted from Jonathan Siemasko's Quneo class
//


LaunchControlXL {
	var <defaultChannel, <numScenes, <numPads, <scInPort, <scOutPort;
	var  <knobs,  <pads, <keys, <faders, <sliders, <global, <transport, <>scenes, <>currentScene=0;

	*new { arg defaultChannel = 8, numScenes=2, numPads = 16, scInPort=nil, scOutPort=nil;
		^super
		.newCopyArgs(defaultChannel, numScenes, numPads, scInPort, scOutPort)
		.init()
	}

	init {
		//Connect to MIDI sources if noone has bothered to do it yet.
		if(MIDIClient.sources.isNil, {MIDIIn.connectAll});
		//Find nanoKONTROL in and out ports
		if(scInPort.isNil, {scInPort = this.detectInPort});
		if(scOutPort.isNil, {scOutPort = this.detectOutPort});

		knobs = Array.newClear(24);
		faders = Array.newClear(8);
		pads = Array.newClear(numPads);
//		global = Array.newClear(4);
		transport = Array.newClear(8); // Transport controls on the left
		scenes= Array.newClear(numScenes);
		this.postInfo;
		SystemClock.sched(0.5,{this.setup});

	}

	postInfo {
		''.postln;
		('LaunchControl XL: In - ' ++ scInPort).postln;
		(Char.tab ++ 'knobs - ' ++ knobs.size).postln;
		(Char.tab ++ 'pads - ' ++ pads.size).postln;
		(Char.tab ++ 'transport - ' ++ transport.size).postln;
		''.postln;
	}

	detectInPort{^(MIDIClient.sources.detect({arg item;
		item.name.find("Launch Control XL").notNil}) !? _.uid ? 0);}

	detectOutPort{^MIDIClient.destinations.find(
		[MIDIClient.destinations.detect({arg item;	item.name.find("Launch Control XL").notNil})?MIDIClient.destinations.detect({arg item;	item.name.find("IAC Bus 1").notNil})]
	)}


	createKnob { arg index, cc, channel;
		var name = \lcknob ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		knobs = knobs.put(index, LKKnob(cc, name, channel, scInPort, index));

	}

	createFader { arg index, cc, channel;
		var name = \lcfader ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		faders = faders.put(index, LKKnob(cc, name, channel, scInPort, index));

	}

	createPad { arg index, cc, channel;
		var name = \lcpad ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		pads = pads.put(index, LKDrumpad(cc, name, channel, scInPort, scOutPort, index));
	}

	createTransportPad { arg index, cc, channel;
		var name = \lctranspad ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		transport = transport.put(index, LKDrumpad(cc, name, channel, scInPort, scOutPort, index,
			Dictionary[\momentary -> [\yellowhi, \black], \toggle -> [\yellowlo, \black], \togHold -> [\yellowlo, \black, \yellowhi]]
		));
	}
/*
	createKeys { arg index, sustain=false, susBus=15, channel;
		var name = \lckeys ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		// keys = keys.put(index, LKKeys(name, channel,sustain, scInPort));
		keys = LKKeys(name, channel,sustain,susBus, scInPort);
	}
*/
	createTransport { arg index, cc, channel;
		var name = \lctransport ++ '_' ++ index;
		if(channel.isNil, {channel = defaultChannel});
		transport = transport.put(index, LKButton(cc, name, channel, scInPort));
	}


	setup {
		this.free;
		8.do({|i|
			this.createKnob(i, 13+i);
			this.createKnob(i+8, 29+i);
			this.createKnob(i+16, 49+i);
		});
		8.do({|i|
			this.createFader(i, 77+i);
		});

		this.createPad(0, 41);
		this.createPad(1, 42);
		this.createPad(2, 43);
		this.createPad(3, 44);
		this.createPad(4, 57);
		this.createPad(5, 58);
		this.createPad(6, 59);
		this.createPad(7, 60);
		this.createPad(8, 73);
		this.createPad(9, 74);
		this.createPad(10, 75);
		this.createPad(11, 76);
		this.createPad(12, 89);
		this.createPad(13, 90);
		this.createPad(14, 91);
		this.createPad(15, 92);


		// Transport buttons are marked top left to bottom right
		this.createTransport(0,104);
		this.createTransport(1,105);
		this.createTransport(2,106);
		this.createTransport(3,107);
		this.createTransportPad(4, 105);
		this.createTransportPad(5, 106);

		if (numPads>16) {  // Backwards compatibility
			this.createGlobalPad(16, 107);
			this.createGlobalPad(17, 108);
		} {
			this.createTransportPad(6, 107);
			this.createTransportPad(7, 108);
		};

		sliders=faders;
		"launchControl XL has been setup".postln;
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
		scenes[sceneIndex]=Array.newClear(numPads);
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
	reset {
		//This is in case LK loses connectivity and needs a reset.
		//Works only if scene had been switched once, or stored with .getScene
		pads[0].reset;
		this.resetScene;
	}
	panic {
		pads.do(_.panic);
		keys.panic;
	}
	free {
		knobs.do(_.free);
		pads.do(_.free);
		keys.free;

	}

}