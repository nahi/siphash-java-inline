public class Benchmark {
    private static final int TIMES = 100000;

    private static final byte[] SPEC_KEY = Utils.bytesOf(
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f
    );

    private static byte[] createMsg(int len) {
	byte[] msg = new byte[len];
	for (int i = 0; i < len; ++i) {
	    msg[i] = (byte)i;
	}
	return msg;
    }
    
    private static long sipHash(byte[] msg) throws Exception {
	SipKey key = new SipKey(SPEC_KEY);
        long start = System.nanoTime();
	for (int i = 0; i < TIMES; ++i) {
	    SipHash.digest(key, msg);
	}
        return System.nanoTime() - start;
    }
    
    private static long sipHashBytes(byte[] msg) throws Exception {
        long start = System.nanoTime();
	for (int i = 0; i < TIMES; ++i) {
	    SipHashBytes.digest(SPEC_KEY, msg);
	}
        return System.nanoTime() - start;
    }
    
    private static long sipHashInline(byte[] msg) throws Exception {
	long k0 = SipHashInlineTry.pack8(SPEC_KEY, 0);
	long k1 = SipHashInlineTry.pack8(SPEC_KEY, 8);
        long start = System.nanoTime();
	for (int i = 0; i < TIMES; ++i) {
	    SipHashInlineTry.digest(k0, k1, msg);
	}
        return System.nanoTime() - start;
    }
    
    private static long sipHashInline2(byte[] msg) throws Exception {
	long k0 = SipHashInlineTry.pack8(SPEC_KEY, 0);
	long k1 = SipHashInlineTry.pack8(SPEC_KEY, 8);
        long start = System.nanoTime();
	for (int i = 0; i < TIMES; ++i) {
	    SipHashInlineTry.digest2(k0, k1, msg);
	}
        return System.nanoTime() - start;
    }
    
    private static long sipHashInline3(byte[] msg) throws Exception {
	long k0 = SipHashInlineTry.pack8(SPEC_KEY, 0);
	long k1 = SipHashInlineTry.pack8(SPEC_KEY, 8);
        long start = System.nanoTime();
	for (int i = 0; i < TIMES; ++i) {
	    SipHashInlineTry.digest3(k0, k1, msg);
	}
        return System.nanoTime() - start;
    }
    
    private static long sipHashInline4(byte[] msg) throws Exception {
	long k0 = SipHashInlineTry.packLong(SPEC_KEY, 0);
	long k1 = SipHashInlineTry.packLong(SPEC_KEY, 8);
        long start = System.nanoTime();
	for (int i = 0; i < TIMES; ++i) {
	    SipHashInlineTry.digest4(k0, k1, msg);
	}
        return System.nanoTime() - start;
    }
    
    private static long murmurHash32(byte[] msg) throws Exception {
	// downcast to 32bit
	int seed = (int)UnsignedInt64.binToIntOffset(SPEC_KEY, 0); 
        long start = System.nanoTime();
	for (int i = 0; i < TIMES; ++i) {
	    MurmurHash.hash32(msg, 0, msg.length, seed);
	}
        return System.nanoTime() - start;
    }
    
    private static long perlHash(byte[] msg) throws Exception {
	// downcast to 32bit
	int seed = (int)UnsignedInt64.binToIntOffset(SPEC_KEY, 0); 
        long start = System.nanoTime();
	for (int i = 0; i < TIMES; ++i) {
	    PerlHash.hash(seed, msg, 0, msg.length);
	}
        return System.nanoTime() - start;
    }

    public static void main(String[] args) throws Exception {
        long start = System.nanoTime();
        while (true) {
	    for (int i = 1; i <= 1024; i *= 2) {
		byte[] msg = createMsg(i);

		System.out.print("" + i + ",SipHash,");
		for (int j = 0; j < 10; ++j) {
		    System.out.print(sipHash(msg) + ",");
		}
		System.out.println();

		System.out.print("" + i + ",SipHashBytes,");
		for (int j = 0; j < 10; ++j) {
		    System.out.print(sipHashBytes(msg) + ",");
		}
		System.out.println();

		System.out.print("" + i + ",SipHashInline,");
		for (int j = 0; j < 10; ++j) {
		    System.out.print(sipHashInline(msg) + ",");
		}
		System.out.println();

		System.out.print("" + i + ",SipHashInline2,");
		for (int j = 0; j < 10; ++j) {
		    System.out.print(sipHashInline2(msg) + ",");
		}
		System.out.println();

		System.out.print("" + i + ",SipHashInline3,");
		for (int j = 0; j < 10; ++j) {
		    System.out.print(sipHashInline3(msg) + ",");
		}
		System.out.println();

		System.out.print("" + i + ",SipHashInline4,");
		for (int j = 0; j < 10; ++j) {
		    System.out.print(sipHashInline4(msg) + ",");
		}
		System.out.println();

		System.out.print("" + i + ",Murmur2,");
		for (int j = 0; j < 10; ++j) {
		    System.out.print(murmurHash32(msg) + ",");
		}
		System.out.println();

		System.out.print("" + i + ",PerlHash,");
		for (int j = 0; j < 10; ++j) {
		    System.out.print(perlHash(msg) + ",");
		}
		System.out.println();

	    }
        }
    }
}
