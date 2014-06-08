public class SipHashInlineTry {

    private static final sun.misc.Unsafe UNSAFE;
    static {
      sun.misc.Unsafe unsafe = null;
      try {
        java.lang.reflect.Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (sun.misc.Unsafe)field.get(null);
      } catch (Exception e) {
        e.printStackTrace();
      }
      UNSAFE = unsafe;
    }
    private static final long base = UNSAFE.arrayBaseOffset(new byte[0].getClass());

    // Assumes data[offset + 7] exists
    public static long packLong(byte[] data, int offset) {
        // read 8 bytes as BE long and reverse to LE
        long m = UNSAFE.getLong(data, base + offset);
        return Long.reverseBytes(m);
    }

    // Assumes data[offset + 7] exists
    public static long pack8(byte[] data, int offset) {
        return
            (long) data[offset    ]       |
            (long) data[offset + 1] <<  8 |
            (long) data[offset + 2] << 16 |
            (long) data[offset + 3] << 24 |
            (long) data[offset + 4] << 32 |
            (long) data[offset + 5] << 40 |
            (long) data[offset + 6] << 48 |
            (long) data[offset + 7] << 56 ;
    }

    // SipHash with hand inlining and Unsafe array access
    public static long digest4(long k0, long k1, byte[] data) {
        long v0 = 0x736f6d6570736575L ^ k0;
        long v1 = 0x646f72616e646f6dL ^ k1;
        long v2 = 0x6c7967656e657261L ^ k0;
        long v3 = 0x7465646279746573L ^ k1;
        long m;
        int dataLength = data.length;
        int last = dataLength / 8 * 8;
        int i = 0;

        // processing 8 bytes blocks in data
        while (i < last) {
            // read 8 bytes as BE long and reverse to LE
            m = UNSAFE.getLong(data, base + i);
            m = Long.reverseBytes(m);
            i += 8;

            // MSGROUND {
                v3 ^= m;

                /* SIPROUND wih hand reordering
                 *
                 * SIPROUND in siphash24.c:
                 *   A: v0 += v1;
                 *   B: v1=ROTL(v1,13);
                 *   C: v1 ^= v0;
                 *   D: v0=ROTL(v0,32);
                 *   E: v2 += v3;
                 *   F: v3=ROTL(v3,16);
                 *   G: v3 ^= v2;
                 *   H: v0 += v3;
                 *   I: v3=ROTL(v3,21);
                 *   J: v3 ^= v0;
                 *   K: v2 += v1;
                 *   L: v1=ROTL(v1,17);
                 *   M: v1 ^= v2;
                 *   N: v2=ROTL(v2,32);
                 *
                 * Each dependency:
                 *   B -> A
                 *   C -> A, B
                 *   D -> C
                 *   F -> E
                 *   G -> E, F
                 *   H -> D, G
                 *   I -> H
                 *   J -> H, I
                 *   K -> C, G
                 *   L -> K
                 *   M -> K, L
                 *   N -> M
                 *
                 * Dependency graph:
                 *   D -> C -> B -> A
                 *        G -> F -> E
                 *   J -> I -> H -> D, G
                 *   N -> M -> L -> K -> C, G
                 *
                 * Resulting parallel friendly execution order:
                 *   -> ABCDHIJ
                 *   -> EFGKLMN
                 */

                // SIPROUND {
                    v0 += v1;                    v2 += v3;
                    v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                    v1 ^= v0;                    v3 ^= v2;
                    v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                    v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                    v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                    v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
                // }
                // SIPROUND {
                    v0 += v1;                    v2 += v3;
                    v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                    v1 ^= v0;                    v3 ^= v2;
                    v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                    v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                    v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                    v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
                // }
                v0 ^= m;
            // }
        }

        // packing the last block to long, as LE 0-7 bytes + the length in the top byte
        m = 0;
        for (i = dataLength - 1; i >= last; --i) {
            m <<= 8; m |= (long) data[i];
        }
        m |= (long) dataLength << 56;
        // MSGROUND {
            v3 ^= m;
            // SIPROUND {
                v0 += v1;                    v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0;                    v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
            // }<D-d>
            // SIPROUND {
                v0 += v1;                    v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0;                    v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
            // }
            v0 ^= m;
        // }

        // finishing...
        v2 ^= 0xff;
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        return v0 ^ v1 ^ v2 ^ v3;
    }

    // SipHash implementation with hand inlining the SIPROUND of siphash24.c
    public static long digest3(long k0, long k1, byte[] data) {
        long v0 = 0x736f6d6570736575L ^ k0;
        long v1 = 0x646f72616e646f6dL ^ k1;
        long v2 = 0x6c7967656e657261L ^ k0;
        long v3 = 0x7465646279746573L ^ k1;
        long m;
        int last = data.length / 8 * 8;
        int i = 0;

        // processing 8 bytes blocks in data
        while (i < last) {
            // pack a block to long, as LE 8 bytes
            m = (long) data[i++]       |
                (long) data[i++] <<  8 |
                (long) data[i++] << 16 |
                (long) data[i++] << 24 |
                (long) data[i++] << 32 |
                (long) data[i++] << 40 |
                (long) data[i++] << 48 |
                (long) data[i++] << 56 ;
            // MSGROUND {
                v3 ^= m;

                /* SIPROUND wih hand reordering
                 *
                 * SIPROUND in siphash24.c:
                 *   A: v0 += v1;
                 *   B: v1=ROTL(v1,13);
                 *   C: v1 ^= v0;
                 *   D: v0=ROTL(v0,32);
                 *   E: v2 += v3;
                 *   F: v3=ROTL(v3,16);
                 *   G: v3 ^= v2;
                 *   H: v0 += v3;
                 *   I: v3=ROTL(v3,21);
                 *   J: v3 ^= v0;
                 *   K: v2 += v1;
                 *   L: v1=ROTL(v1,17);
                 *   M: v1 ^= v2;
                 *   N: v2=ROTL(v2,32);
                 *
                 * Each dependency:
                 *   B -> A
                 *   C -> A, B
                 *   D -> C
                 *   F -> E
                 *   G -> E, F
                 *   H -> D, G
                 *   I -> H
                 *   J -> H, I
                 *   K -> C, G
                 *   L -> K
                 *   M -> K, L
                 *   N -> M
                 *
                 * Dependency graph:
                 *   D -> C -> B -> A
                 *        G -> F -> E
                 *   J -> I -> H -> D, G
                 *   N -> M -> L -> K -> C, G
                 *
                 * Resulting parallel friendly execution order:
                 *   -> ABCDHIJ
                 *   -> EFGKLMN
                 */

                // SIPROUND {
                    v0 += v1;                    v2 += v3;
                    v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                    v1 ^= v0;                    v3 ^= v2;
                    v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                    v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                    v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                    v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
                // }
                // SIPROUND {
                    v0 += v1;                    v2 += v3;
                    v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                    v1 ^= v0;                    v3 ^= v2;
                    v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                    v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                    v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                    v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
                // }
                v0 ^= m;
            // }
        }

        // packing the last block to long, as LE 0-7 bytes + the length in the top byte
        m = 0;
        for (i = data.length - 1; i >= last; --i) {
            m <<= 8; m |= (long) data[i];
        }
        m |= (long) data.length << 56;
        // MSGROUND {
            v3 ^= m;
            // SIPROUND {
                v0 += v1;                    v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0;                    v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
            // }
            // SIPROUND {
                v0 += v1;                    v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0;                    v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
                v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
                v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
            // }
            v0 ^= m;
        // }

        // finishing...
        v2 ^= 0xff;
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        // SIPROUND {
            v0 += v1;                    v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51; v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0;                    v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32; v2 += v1;
            v0 += v3;                    v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43; v1 ^= v2;
            v3 ^= v0;                    v2 = (v2 << 32) | v2 >>> 32;
        // }
        return v0 ^ v1 ^ v2 ^ v3;
    }

    public static long digest2(long k0, long k1, byte[] data) {
        long v0 = 0x736f6d6570736575L ^ k0;
        long v1 = 0x646f72616e646f6dL ^ k1;
        long v2 = 0x6c7967656e657261L ^ k0;
        long v3 = 0x7465646279746573L ^ k1;
        long m;

        int i = 0;
        int last = data.length / 8 * 8;
        while (i <= last) {
            if (i < last) {
                m = (long) data[i++]       |
                    (long) data[i++] <<  8 |
                    (long) data[i++] << 16 |
                    (long) data[i++] << 24 |
                    (long) data[i++] << 32 |
                    (long) data[i++] << 40 |
                    (long) data[i++] << 48 |
                    (long) data[i++] << 56 ;
            } else {
                m = 0;
                for (int j = data.length - 1; j >= i; --j) {
                    m = (m << 8) | (long) data[j];
                }
                m |= (long) data.length << 56;
                i = last + 1;
            }
            v3 ^= m;

            // SIPROUND>
            v0 += v1; v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51;
            v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0; v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32;
            v2 += v1; v0 += v3;
            v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43;
            v1 ^= v2; v3 ^= v0;
            v2 = (v2 << 32) | v2 >>> 32;
            // <SIPROUND

            // SIPROUND>
            v0 += v1; v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51;
            v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0; v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32;
            v2 += v1; v0 += v3;
            v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43;
            v1 ^= v2; v3 ^= v0;
            v2 = (v2 << 32) | v2 >>> 32;
            // <SIPROUND

            v0 ^= m;
        }

        v2 ^= 0xff;

        // SIPROUND>
        v0 += v1; v2 += v3;
        v1 = (v1 << 13) | v1 >>> 51;
        v3 = (v3 << 16) | v3 >>> 48;
        v1 ^= v0; v3 ^= v2;
        v0 = (v0 << 32) | v0 >>> 32;
        v2 += v1; v0 += v3;
        v1 = (v1 << 17) | v1 >>> 47;
        v3 = (v3 << 21) | v3 >>> 43;
        v1 ^= v2; v3 ^= v0;
        v2 = (v2 << 32) | v2 >>> 32;
        // <SIPROUND

        // SIPROUND>
        v0 += v1; v2 += v3;
        v1 = (v1 << 13) | v1 >>> 51;
        v3 = (v3 << 16) | v3 >>> 48;
        v1 ^= v0; v3 ^= v2;
        v0 = (v0 << 32) | v0 >>> 32;
        v2 += v1; v0 += v3;
        v1 = (v1 << 17) | v1 >>> 47;
        v3 = (v3 << 21) | v3 >>> 43;
        v1 ^= v2; v3 ^= v0;
        v2 = (v2 << 32) | v2 >>> 32;
        // <SIPROUND

        // SIPROUND>
        v0 += v1; v2 += v3;
        v1 = (v1 << 13) | v1 >>> 51;
        v3 = (v3 << 16) | v3 >>> 48;
        v1 ^= v0; v3 ^= v2;
        v0 = (v0 << 32) | v0 >>> 32;
        v2 += v1; v0 += v3;
        v1 = (v1 << 17) | v1 >>> 47;
        v3 = (v3 << 21) | v3 >>> 43;
        v1 ^= v2; v3 ^= v0;
        v2 = (v2 << 32) | v2 >>> 32;
        // <SIPROUND

        // SIPROUND>
        v0 += v1; v2 += v3;
        v1 = (v1 << 13) | v1 >>> 51;
        v3 = (v3 << 16) | v3 >>> 48;
        v1 ^= v0; v3 ^= v2;
        v0 = (v0 << 32) | v0 >>> 32;
        v2 += v1; v0 += v3;
        v1 = (v1 << 17) | v1 >>> 47;
        v3 = (v3 << 21) | v3 >>> 43;
        v1 ^= v2; v3 ^= v0;
        v2 = (v2 << 32) | v2 >>> 32;
        // <SIPROUND

        return v0 ^ v1 ^ v2 ^ v3;
    }

    public static long digest(long k0, long k1, byte[] data) {
        long v0 = 0x736f6d6570736575L ^ k0;
        long v1 = 0x646f72616e646f6dL ^ k1;
        long v2 = 0x6c7967656e657261L ^ k0;
        long v3 = 0x7465646279746573L ^ k1;
        long m;
        int i;

        int iter = data.length / 8;
        for (i = 0; i < iter; ++i) {
            m = pack8(data, i * 8);
            // ITER>
                v3 ^= m;
                // SIPROUND>
                v0 += v1; v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51;
                v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0; v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32;
                v2 += v1; v0 += v3;
                v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43;
                v1 ^= v2; v3 ^= v0;
                v2 = (v2 << 32) | v2 >>> 32;
                // <SIPROUND
                // SIPROUND>
                v0 += v1; v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51;
                v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0; v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32;
                v2 += v1; v0 += v3;
                v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43;
                v1 ^= v2; v3 ^= v0;
                v2 = (v2 << 32) | v2 >>> 32;
                // <SIPROUND
                v0 ^= m;
            // <ITER
        }
        // LASTBLOCK>
            int last = iter * 8;
            m = 0;
            for (i = data.length - 1; i >= last; --i) {
                m = (m << 8) | (long) data[i];
            }
            m |= (long) data.length << 56;
            // ITER>
                v3 ^= m;
                // SIPROUND>
                v0 += v1; v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51;
                v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0; v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32;
                v2 += v1; v0 += v3;
                v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43;
                v1 ^= v2; v3 ^= v0;
                v2 = (v2 << 32) | v2 >>> 32;
                // <SIPROUND
                // SIPROUND>
                v0 += v1; v2 += v3;
                v1 = (v1 << 13) | v1 >>> 51;
                v3 = (v3 << 16) | v3 >>> 48;
                v1 ^= v0; v3 ^= v2;
                v0 = (v0 << 32) | v0 >>> 32;
                v2 += v1; v0 += v3;
                v1 = (v1 << 17) | v1 >>> 47;
                v3 = (v3 << 21) | v3 >>> 43;
                v1 ^= v2; v3 ^= v0;
                v2 = (v2 << 32) | v2 >>> 32;
                // <SIPROUND
                v0 ^= m;
            // <ITER
        // <LASTBLOCK

        // FINISH>
            v2 ^= 0xff;
            // SIPROUND>
            v0 += v1; v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51;
            v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0; v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32;
            v2 += v1; v0 += v3;
            v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43;
            v1 ^= v2; v3 ^= v0;
            v2 = (v2 << 32) | v2 >>> 32;
            // <SIPROUND
            // SIPROUND>
            v0 += v1; v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51;
            v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0; v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32;
            v2 += v1; v0 += v3;
            v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43;
            v1 ^= v2; v3 ^= v0;
            v2 = (v2 << 32) | v2 >>> 32;
            // <SIPROUND
            // SIPROUND>
            v0 += v1; v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51;
            v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0; v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32;
            v2 += v1; v0 += v3;
            v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43;
            v1 ^= v2; v3 ^= v0;
            v2 = (v2 << 32) | v2 >>> 32;
            // <SIPROUND
            // SIPROUND>
            v0 += v1; v2 += v3;
            v1 = (v1 << 13) | v1 >>> 51;
            v3 = (v3 << 16) | v3 >>> 48;
            v1 ^= v0; v3 ^= v2;
            v0 = (v0 << 32) | v0 >>> 32;
            v2 += v1; v0 += v3;
            v1 = (v1 << 17) | v1 >>> 47;
            v3 = (v3 << 21) | v3 >>> 43;
            v1 ^= v2; v3 ^= v0;
            v2 = (v2 << 32) | v2 >>> 32;
            // <SIPROUND
        // <FINISH

        return v0 ^ v1 ^ v2 ^ v3;
    }
}
