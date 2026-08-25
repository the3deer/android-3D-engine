package org.the3deer.android.engine.text;

import org.the3deer.android.engine.model.Screen;
import org.the3deer.util.bean.Bean;
import org.the3deer.util.bean.BeanInit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Professional Vector Font Factory using authentic Hershey Simplex Roman data.
 * 
 * ### Custom Overrides Guide (decodeCustom method):
 * We use a simplified 7x5/7x7 coordinate grid for custom fixes.
 * String format: "X1 Y1 X2 Y2 ..." (each digit is a grid point 0-6).
 * 
 * Grid Layout:
 * (0,0) (1,0) (2,0) (3,0) (4,0) (5,0) (6,0)  <- TOP
 * (0,1) (1,1) (2,1) (3,1) (4,1) (5,1) (6,1)
 * (0,2) (1,2) (2,2) (3,2) (4,2) (5,2) (6,2)
 * (0,3) (1,3) (2,3) (3,3) (4,3) (5,3) (6,3)  <- MIDDLE LINE
 * (0,4) (1,4) (2,4) (3,4) (4,4) (5,4) (6,4)
 * (0,5) (1,5) (2,5) (3,5) (4,5) (5,5) (6,5)
 * (0,6) (1,6) (2,6) (3,6) (4,6) (5,6) (6,6)  <- BOTTOM (Baseline)
 * 
 * Pen-Up (Jump) Logic:
 * To "lift the pen," repeat the last coordinate of the stroke.
 * Example for 'H': "1016 1613 1353 5350 5056"
 * 1. Stroke: (1,0) to (1,6)
 * 2. Jump: Repeat (1,6) and start next at (1,3)
 * 3. Stroke: (1,3) to (5,3)
 * 4. Jump: Repeat (5,3) and start next at (5,0) ... and so on.
 */
@Bean(name = "hershey")
public class FontFactoryHershey implements FontFactory {

    private static final Logger logger = Logger.getLogger(FontFactoryHershey.class.getSimpleName());

    @Inject
    private Screen screen;

    private float charWidth = 48;
    private float charHeight = 48;
    private final float scale = 1.8f; // Hershey Simplex is ~21 units high, scale to ~40px

    private final Map<Integer, String> hersheyData = new HashMap<>();
    private final Map<Character, float[]> glyphCache = new HashMap<>();
    private final Map<Character, Integer> asciiToHershey = new HashMap<>();

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    @BeanInit
    public void setUp() {
        initHersheyData();
        initCustomOverrides(); // Our hand-optimized fixes
        initAsciiMapping();
        cacheGlyphs();
        
        // Debugging '4' as requested
        logNumericGlyph('4');
    }

    private void logNumericGlyph(char c) {
        Integer hersheyId = asciiToHershey.get(c);
        if (hersheyId != null) {
            String hData = hersheyData.get(hersheyId);
            logger.info("Hershey '" + c + "' string: " + hData);
        }
    }

    private void initHersheyData() {
        // Uppercase
        hersheyData.put(501, "I[RFJ[ RRFZ[ RMTWT"); // A
        hersheyData.put(502, "G\\KFK[ RKFTFWGXHYJYLXNWOTP RKPTPWQXRYTYWXYWZT[K["); // B
        hersheyData.put(503, "H]ZKYIWGUFQFOGMILKKNKSLVMXOZQ[U[WZYXZV"); // C
        hersheyData.put(504, "G\\KFK[ RKFRFUGWIXKYNYSXVWXUZR[K["); // D
        hersheyData.put(505, "H[LFL[ RLFYF RLPTP RL[Y["); // E
        hersheyData.put(506, "HZLFL[ RLFYF RLPTP"); // F
        hersheyData.put(507, "H]ZKYIWGUFQFOGMILKKNKSLVMXOZQ[U[WZYXZVZS RUSZS"); // G
        hersheyData.put(508, "G]KFK[ RYFY[ RKPYP"); // H
        hersheyData.put(509, "NVRFR["); // I
        hersheyData.put(510, "JZVFVVUYTZR[P[NZMYLVLT"); // J
        hersheyData.put(511, "G\\KFK[ RYFKT RPOY["); // K
        hersheyData.put(512, "HYLFL[ RL[X["); // L
        hersheyData.put(513, "F^JFJ[ RJFR[ RZFR[ RZFZ["); // M
        hersheyData.put(514, "G]KFK[ RKFY[ RYFY["); // N
        hersheyData.put(515, "G]PFNGLIKKJNJSKVLXNZP[T[VZXXYVZSZNYKXIVGTFPF"); // O
        hersheyData.put(516, "G\\KFK[ RKFTFWGXHYJYMXOWPTQKQ"); // P
        hersheyData.put(517, "G]PFNGLIKKJNJSKVLXNZP[T[VZXXYVZSZNYKXIVGTFPF RSWY]"); // Q
        hersheyData.put(518, "G\\KFK[ RKFTFWGXHYJYLXNWOTPKP RRPY["); // R
        hersheyData.put(519, "H\\YIWGTFPFMGKIKKLMMNOOUQWRXSYUYXWZT[P[MZKX"); // S
        hersheyData.put(520, "JZRFR[ RKFYF"); // T
        hersheyData.put(521, "G]KFKULXNZQ[S[VZXXYUYF"); // U
        hersheyData.put(522, "I[JFR[ RZFR["); // V
        hersheyData.put(523, "F^HFM[ RRFM[ RRFW[ R\\FW["); // W
        hersheyData.put(524, "H\\KFY[ RYFK["); // X
        hersheyData.put(525, "I[JFRPR[ RZFRP"); // Y
        hersheyData.put(526, "H\\YFK[ RKFYF RK[Y["); // Z

        // Lowercase
        hersheyData.put(601, "I\\XMX[ RXPVNTMQMONMPLSLUMXOZQ[T[VZXX"); // a
        hersheyData.put(602, "H[LFL[ RLPNNPMSMUNWPXSXUWXUZS[P[NZLX"); // b
        hersheyData.put(603, "I[XPVNTMQMONMPLSLUMXOZQ[T[VZXX"); // c
        hersheyData.put(604, "I\\XFX[ RXPVNTMQMONMPLSLUMXOZQ[T[VZXX"); // d
        hersheyData.put(605, "I[LSXSXQWOVNTMQMONMPLSLUMXOZQ[T[VZXX"); // e
        hersheyData.put(606, "MYWFUFSGRJR[ ROMVM"); // f
        hersheyData.put(607, "I\\XMX]W`VaTbQbOa RXPVNTMQMONMPLSLUMXOZQ[T[VZXX"); // g
        hersheyData.put(608, "I\\MFM[ RMQPNRMUMWNXQX["); // h
        hersheyData.put(609, "NVQFRGSFREQF RRMR["); // i
        hersheyData.put(610, "MWRFSGTFSERF RSMS^RaPbNb"); // j
        hersheyData.put(611, "IZMFM[ RWMMW RQSX["); // k
        hersheyData.put(612, "NVRFR["); // l
        hersheyData.put(613, "CaGMG[ RGQJNLMOMQNRQR[ RRQUNWMZM\\N]Q]["); // m
        hersheyData.put(614, "I\\MMM[ RMQPNRMUMWNXQX["); // n
        hersheyData.put(615, "I\\QMONMPLSLUMXOZQ[T[VZXXYUYSXPVNTMQM"); // o
        hersheyData.put(616, "H[LMLb RLPNNPMSMUNWPXSXUWXUZS[P[NZLX"); // p
        hersheyData.put(617, "I\\XMXb RXPVNTMQMONMPLSLUMXOZQ[T[VZXX"); // q
        hersheyData.put(618, "KXOMO[ ROSPPRNTMWM"); // r
        hersheyData.put(619, "J[XPWNTMQMNNMPNRPSUTWUXWXXWZT[Q[NZMX"); // s
        hersheyData.put(620, "MYRFRWSZU[W[ ROMVM"); // t
        hersheyData.put(621, "I\\MMMWNZP[S[UZXW RXMX["); // u
        hersheyData.put(622, "JZLMR[ RXMR["); // v
        hersheyData.put(623, "G]JMN[ RRMN[ RRMV[ RZMV["); // w
        hersheyData.put(624, "J[MMX[ RXMM["); // x
        hersheyData.put(625, "JZLMR[ RXMR[P_NaLbKb"); // y
        hersheyData.put(626, "J[XMM[ RMMXM RM[X["); // z

        // Digits
        hersheyData.put(700, "H\\QFNGLJKOKRLWNZQ[S[VZXWYRYOXJVGSFQF");
        hersheyData.put(701, "H\\NJPISFS[");
        hersheyData.put(702, "H\\LKLJMHNGPFTFVGWHXJXLWNUQK[Y[");
        hersheyData.put(703, "H\\MFXFRNUNWOXPYSYUXXVZS[P[MZLYKW");
        hersheyData.put(704, "H\\UFKTZT RUFU[");
        hersheyData.put(705, "H\\WFMFLOMNPMSMVNXPYSYUXXVZS[P[MZLYKW");
        hersheyData.put(706, "H\\XIWGTFRFOGMJLOLTMXOZR[S[VZXXYUYTXQVOSNRNOOMQLT");
        hersheyData.put(707, "H\\YFO[ RKFYF");
        hersheyData.put(708, "H\\PFMGLILKMMONSOVPXRYTYWXYWZT[P[MZLYKWKTLRNPQOUNWMXKXIWGTFPF");
        hersheyData.put(709, "H\\XMWPURRSQSNRLPKMKLLINGQFRFUGWIXMXRWWUZR[P[MZLX");

        // Symbols
        hersheyData.put(699, "JZ"); // Space
        hersheyData.put(710, "MWRYQZR[SZRY"); // .
        hersheyData.put(711, "MWSZR[QZRYSZS\\R^Q_"); // ,
        hersheyData.put(712, "MWRMQNROSNRM RRYQZR[SZRY"); // :
        hersheyData.put(713, "MWRMQNROSNRM RSZR[QZRYSZS\\R^Q_"); // ;
        hersheyData.put(714, "MWRFRT RRYQZR[SZRY"); // ?
        hersheyData.put(717, "JZNFNM RVFVM"); // "
        hersheyData.put(718, "KYQFOGNINKOMQNSNUMVKVIUGSFQF"); // '
        hersheyData.put(721, "KYVBTDRGPKOPOTPYR]T`Vb"); // (
        hersheyData.put(722, "KYNBPDRGTKUPUTTYR]P`Nb"); // )
        hersheyData.put(724, "E_IR[R"); // +
        hersheyData.put(725, "E_RIR[ RIR[R"); // -
        hersheyData.put(731, "MWRHQGRFSGSIRKQL"); // _
    }

    /**
     *  * Grid Layout:
     *  * (0,0) (1,0) (2,0) (3,0) (4,0) (5,0) (6,0)  <- TOP
     *  * (0,1) (1,1) (2,1) (3,1) (4,1) (5,1) (6,1)
     *  * (0,2) (1,2) (2,2) (3,2) (4,2) (5,2) (6,2)
     *  * (0,3) (1,3) (2,3) (3,3) (4,3) (5,3) (6,3)  <- MIDDLE LINE
     *  * (0,4) (1,4) (2,4) (3,4) (4,4) (5,4) (6,4)
     *  * (0,5) (1,5) (2,5) (3,5) (4,5) (5,5) (6,5)
     *  * (0,6) (1,6) (2,6) (3,6) (4,6) (5,6) (6,6)  <- BOTTOM (Baseline)
     */
    private void initCustomOverrides() {
        // Uppercase Fixes (Readable Numeric Grid with Space for Jumps)
/*        glyphCache.put('A', decodeCustom("163056 2343"));
        glyphCache.put('E', decodeCustom("50101656 1343"));
        glyphCache.put('F', decodeCustom("161050 1343"));
        glyphCache.put('H', decodeCustom("1016 5056 1353"));
        glyphCache.put('K', decodeCustom("1016 1340 1346"));
        glyphCache.put('Q', decodeCustom("2011152636454120 3355"));
        glyphCache.put('T', decodeCustom("0050 2026"));
        glyphCache.put('X', decodeCustom("1046 4016"));
        glyphCache.put('Y', decodeCustom("1033 5033 3336"));
        glyphCache.put('Z', decodeCustom("1050 1656"));

        // Lowercase & Digits Fixes
        glyphCache.put('4', decodeCustom("46401353"));
        //glyphCache.put('g', decodeCustom("41111040 4144 14"));
        glyphCache.put('t', decodeCustom("2026 1232"));
        glyphCache.put('x', decodeCustom("1246 4216"));*/
        //glyphCache.put('z', decodeCustom("1141 1444"));
    }

    private float[] decodeCustom(String path) {
        if (path == null || path.isEmpty()) return new float[0];
        
        List<Float> vertices = new ArrayList<>();
        float lastX = -1, lastY = -1;
        boolean jumpNext = false;

        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (Character.isWhitespace(c)) {
                jumpNext = true;
                continue;
            }

            if (Character.isDigit(c) && i + 1 < path.length()) {
                int ix = Character.getNumericValue(c);
                int iy = Character.getNumericValue(path.charAt(++i));

                // Logic: hY = gridPoint * 3.5 - 12.0
                float hX = (ix - 3) * 3.5f;
                float hY = (iy * 3.5f) - 12.0f;

                float x = hX * scale + (charWidth / 2);
                float y = -hY * scale + (charHeight / 2);

                if (jumpNext) {
                    if (!vertices.isEmpty()) {
                        // Lift Pen: Sentinel Z Handshake
                        vertices.add(lastX); vertices.add(lastY); vertices.add(-1.0f);
                        vertices.add(x); vertices.add(y); vertices.add(-1.0f);
                    }
                    jumpNext = false;
                }

                vertices.add(x); vertices.add(y); vertices.add(0f);
                lastX = x; lastY = y;
            }
        }

        float[] result = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) result[i] = vertices.get(i);
        return result;
    }

    private void initAsciiMapping() {
        for (int i = 0; i < 26; i++) {
            asciiToHershey.put((char)('A' + i), 501 + i);
            asciiToHershey.put((char)('a' + i), 601 + i);
        }
        for (int i = 0; i < 10; i++) asciiToHershey.put((char)('0' + i), 700 + i);
        asciiToHershey.put(' ', 699);
        asciiToHershey.put('.', 710);
        asciiToHershey.put(',', 711);
        asciiToHershey.put(':', 712);
        asciiToHershey.put(';', 713);
        asciiToHershey.put('?', 714);
        asciiToHershey.put('"', 717);
        asciiToHershey.put('\'', 718);
        asciiToHershey.put('(', 721);
        asciiToHershey.put(')', 722);
        asciiToHershey.put('+', 724);
        asciiToHershey.put('-', 725);
        asciiToHershey.put('_', 731);
    }

    private void cacheGlyphs() {
        for (Map.Entry<Character, Integer> entry : asciiToHershey.entrySet()) {
            if (!glyphCache.containsKey(entry.getKey())) {
                String path = hersheyData.get(entry.getValue());
                if (path != null) {
                    glyphCache.put(entry.getKey(), decodeHershey(path));
                }
            }
        }
    }

    private float[] decodeHershey(String path) {
        if (path == null || path.length() < 2) return new float[0];

        final List<Float> vertices = new ArrayList<>();
        float lastX = -1, lastY = -1;
        boolean jumping = false;

        for (int i = 2; i < path.length() - 1; i += 2) {
            final char cx = path.charAt(i);
            final char cy = path.charAt(i + 1);

            if (cx == ' ' && cy == 'R') {
                jumping = true;
                continue;
            }

            final float x = (cx - 'R') * scale + (charWidth / 2);
            final float y = -(cy - 'R') * scale + (charHeight / 2);

            if (jumping) {
                if (!vertices.isEmpty()) {
                    // Lift Pen: Sentinel Z Handshake
                    vertices.add(lastX); vertices.add(lastY); vertices.add(-1.0f);
                    vertices.add(x); vertices.add(y); vertices.add(-1.0f);
                }
                jumping = false;
            }

            vertices.add(x); vertices.add(y); vertices.add(0f);
            lastX = x; lastY = y;
        }

        final float[] result = new float[vertices.size()];
        for (int i = 0; i < vertices.size(); i++) result[i] = vertices.get(i);
        return result;
    }

    @Override
    public float[] getChar(char c) {
        float[] glyph = glyphCache.get(c);
        return glyph != null ? glyph : glyphCache.get(' ');
    }

    @Override
    public float[] getSymbol(String s) {
        return new float[0];
    }

    public float getCharWidth() { return charWidth; }
    public float getCharHeight() { return charHeight; }
}
