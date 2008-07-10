/**
 *	(C)opyright 2007-2008 by Hanns Holger Rutz. All rights reserved.
 *
 *	@version	0.13, 10-Sep-07
 */
BosqueBusConfig {
	var <busCfgID;
	var <forest;
	var <numInputs, <numOutputs;
	var connections;
	var <bus;
	var <synthPan, <synthMix;
	var <name;
	
	var <groupPan, <groupMix;
	
	var debugSynthDefs = false;

	*new { arg busCfgID, numInputs, numOutputs;
		var forest, doc, busConfigs;
		
		forest		= Bosque.default;
		doc			= forest.session;
		busConfigs	= doc.busConfigs;
		busConfigs.do({ arg b; if( b.busCfgID == busCfgID, { ^b })});
		
		^super.new.prInit( busCfgID, numInputs, numOutputs );
	}
	
	prInit { arg argID, argNumInputs, argNumOutputs;
		busCfgID		= argID;
		forest		= Bosque.default;
		numInputs		= argNumInputs;
		numOutputs	= argNumOutputs;
		
		forest.doWhenScSynthBooted({ this.prAudioInit });
	}

	connections_ { arg conn;
		// stupidly connections_ gets called when loading document
		// for each track the busconfig is used in... so filter it!
		if( connections == conn, { ^this });

		if( (conn.size != numInputs) or: { conn.detect({ arg set; set.size != numOutputs }).notNil }, {
			MethodError( "Connections matrix doesn't match numInputs / numOutputs", thisMethod ).throw;
		});
		
		connections	= conn.copy;
//		[ "connections_", name, connections ].postln;
		if( bus.notNil, {
			this.prCreateConnSynths;
		});
		this.changed( \connections );
	}
	
	name_ { arg str;
		name = str;
		this.changed( \name );
	}
	
	// use with care!!!
	busCfgID_ { arg id;
		busCfgID = id;		
	}

	editRename { arg source, newName, ce;
		var oldName = name;
		ce.addPerform( BosqueFunctionEdit({ this.name = newName }, { this.name = oldName }, "Change Bus Name", true ));
	}

	editConnections { arg source, newConn, ce;
		var oldConn = connections;
		newConn = newConn.copy;
		ce.addPerform( BosqueFunctionEdit({ this.connections = newConn }, { this.connections = oldConn }, "Change Bus Connections", true ));
	}

	connections { ^connections.copy }

	storeArgs { ^[ busCfgID, numInputs, numOutputs ]}

	storeModifiersOn { arg stream;
		if( name.notNil, {
			stream << ".name_(";
			name.storeOn( stream );
			stream << ")";
		});
		if( connections.notNil, {
			stream << ".connections_(";
			connections.storeOn( stream );
			stream << ")";
		});
	}

	prAudioInit {
//		("prAudioInit : " ++ name).postln;
		bus		= Bus.audio( forest.scsynth, max( numInputs, numOutputs ));
		groupPan	= Group( forest.panGroup );
		groupMix	= Group( forest.mixGroup );
		
		if( connections.notNil, { ^this.prCreateConnSynths });
	}
	
	prCreateConnSynths {
		var bundle, defNamePan, defNameMix, defPan, defMix, numMixChans;
		
//		groupMix.freeAll;
//		groupPan.freeAll;
		
//		("prCreateConnSynths : " ++ name).postln;
//		if( name.asString == "BusStSides", {
//			this.dumpBackTrace;
//		});

		defNamePan = \forestBus ++ busCfgID;
		defPan = SynthDef( defNamePan, { arg bus;
			var inp, outp;
			
			inp 	= In.ar( bus, numInputs ).asArray; // .asArray fuer mono !!
			outp	= 0.0 ! numOutputs;
			connections.do({ arg set, i;
				set.do({ arg gain, j;
					// very efficient as BinaryOpUGen uses optimizations
					outp[ j ] = outp[ j ] + (inp[ i ] * gain);
				});
			});
			ReplaceOut.ar( bus, outp );	// 0.0 get replaced with Silent.ar !
		});
		if( debugSynthDefs, { Kurs.viewSynthDef( defPan )});
		numMixChans = min( numOutputs, forest.masterBus.numChannels );
		defNameMix = \forestMix ++ numMixChans;
		defMix = SynthDef( defNameMix, { arg inBus, outBus;
			Out.ar( outBus, In.ar( inBus, numMixChans ));
		});

		bundle = OSCBundle.new;
		bundle.addPrepare([ "/d_recv", defPan.asBytes ]);
		bundle.addPrepare([ "/d_recv", defMix.asBytes ]);
		synthPan = Synth.basicNew( defNamePan, forest.scsynth );
		synthMix = Synth.basicNew( defNameMix, forest.scsynth );
		bundle.add( groupMix.freeAllMsg );
		bundle.add( groupPan.freeAllMsg );
		bundle.add( synthPan.newMsg( groupPan, [ \bus, bus.index ]));
		bundle.add( synthMix.newMsg( groupMix, [ \inBus, bus.index, \outBus,  forest.masterBus.index ]));
		bundle.send( forest.scsynth ); // , bufferLatency
	}

	dispose {
//		if( java.notNil, { java.destroy });
		groupMix.free; groupMix = nil;
		groupPan.free; groupPan = nil;
//		if( synthMix.notNil, { synthMix.free; synthMix = nil });
//		if( synthPan.notNil, { synthPan.free; synthPan = nil });
		bus.free; bus = nil;
	}
}