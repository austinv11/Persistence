package com.austinv11.persistence;

/**
 * These represent the opcodes usable. The ordinal value of each constant corresponds to the payload number.
 */
public enum OpCode {
	IDENTIFY, 
	OK,
	REJECTION,
	PING,
	PONG,
	KICK,
	INITIALIZE,
	CREATION,
	CHANGE,
	REMOVAL
}
