
////////////////////////////////////////////////////////////////////////////////
// Copyright (c) 2007 IBM Corporation.
// All rights reserved. This program and the accompanying materials
// are made available under the terms of the Eclipse Public License v1.0
// which accompanies this distribution, and is available at
// http://www.eclipse.org/legal/epl-v10.html
//
//Contributors:
//    Philippe Charles (pcharles@us.ibm.com) - initial API and implementation
////////////////////////////////////////////////////////////////////////////////

package org.eclipse.imp.prefspecs.parser;

public class PrefspecsLexerprs implements lpg.runtime.ParseTable, PrefspecsLexersym {
    public final static int ERROR_SYMBOL = 0;
    public final int getErrorSymbol() { return ERROR_SYMBOL; }

    public final static int SCOPE_UBOUND = 0;
    public final int getScopeUbound() { return SCOPE_UBOUND; }

    public final static int SCOPE_SIZE = 0;
    public final int getScopeSize() { return SCOPE_SIZE; }

    public final static int MAX_NAME_LENGTH = 0;
    public final int getMaxNameLength() { return MAX_NAME_LENGTH; }

    public final static int NUM_STATES = 14;
    public final int getNumStates() { return NUM_STATES; }

    public final static int NT_OFFSET = 102;
    public final int getNtOffset() { return NT_OFFSET; }

    public final static int LA_STATE_OFFSET = 787;
    public final int getLaStateOffset() { return LA_STATE_OFFSET; }

    public final static int MAX_LA = 1;
    public final int getMaxLa() { return MAX_LA; }

    public final static int NUM_RULES = 240;
    public final int getNumRules() { return NUM_RULES; }

    public final static int NUM_NONTERMINALS = 47;
    public final int getNumNonterminals() { return NUM_NONTERMINALS; }

    public final static int NUM_SYMBOLS = 149;
    public final int getNumSymbols() { return NUM_SYMBOLS; }

    public final static int SEGMENT_SIZE = 8192;
    public final int getSegmentSize() { return SEGMENT_SIZE; }

    public final static int START_STATE = 241;
    public final int getStartState() { return START_STATE; }

    public final static int IDENTIFIER_SYMBOL = 0;
    public final int getIdentifier_SYMBOL() { return IDENTIFIER_SYMBOL; }

    public final static int EOFT_SYMBOL = 100;
    public final int getEoftSymbol() { return EOFT_SYMBOL; }

    public final static int EOLT_SYMBOL = 103;
    public final int getEoltSymbol() { return EOLT_SYMBOL; }

    public final static int ACCEPT_ACTION = 546;
    public final int getAcceptAction() { return ACCEPT_ACTION; }

    public final static int ERROR_ACTION = 547;
    public final int getErrorAction() { return ERROR_ACTION; }

    public final static boolean BACKTRACK = false;
    public final boolean getBacktrack() { return BACKTRACK; }

    public final int getStartSymbol() { return lhs(0); }
    public final boolean isValidForParser() { return PrefspecsLexersym.isValidForParser; }


    public interface IsNullable {
        public final static byte isNullable[] = {0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,1,1,0,0,0
        };
    };
    public final static byte isNullable[] = IsNullable.isNullable;
    public final boolean isNullable(int index) { return isNullable[index] != 0; }

    public interface ProsthesesIndex {
        public final static byte prosthesesIndex[] = {0,
            10,9,21,22,23,24,25,26,27,28,
            29,30,31,32,33,34,35,36,37,38,
            39,40,41,42,43,44,45,46,4,14,
            15,17,18,19,2,3,5,6,7,8,
            11,12,13,16,20,47,1
        };
    };
    public final static byte prosthesesIndex[] = ProsthesesIndex.prosthesesIndex;
    public final int prosthesesIndex(int index) { return prosthesesIndex[index]; }

    public interface IsKeyword {
        public final static byte isKeyword[] = {0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0
        };
    };
    public final static byte isKeyword[] = IsKeyword.isKeyword;
    public final boolean isKeyword(int index) { return isKeyword[index] != 0; }

    public interface BaseCheck {
        public final static byte baseCheck[] = {0,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,2,1,2,2,2,
            1,2,3,1,1,3,0,1,2,1,
            1,1,1,1,3,0,1,2,1,1,
            1,1,1,1,2,2,2,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1,
            1,1,1,1,1,1,1,1,1,1
        };
    };
    public final static byte baseCheck[] = BaseCheck.baseCheck;
    public final int baseCheck(int index) { return baseCheck[index]; }
    public final static byte rhs[] = baseCheck;
    public final int rhs(int index) { return rhs[index]; };

    public interface BaseAction {
        public final static char baseAction[] = {
            35,35,35,35,35,35,35,35,35,35,
            35,35,35,35,35,35,35,36,36,36,
            36,29,29,37,38,38,42,43,43,43,
            30,30,30,30,30,41,44,44,44,32,
            32,32,32,32,39,39,40,40,1,1,
            1,1,1,1,1,1,1,1,3,3,
            4,4,5,5,6,6,7,7,8,8,
            9,9,10,10,11,11,12,12,13,13,
            14,14,15,15,16,16,17,17,18,18,
            19,19,20,20,21,21,22,22,23,23,
            24,24,25,25,26,26,27,27,28,28,
            2,2,2,2,2,2,2,2,2,2,
            2,2,2,2,2,2,2,2,2,2,
            2,2,2,2,2,2,34,34,34,34,
            34,46,46,46,46,46,46,46,46,46,
            46,46,46,46,46,46,46,46,46,46,
            46,46,46,46,46,46,46,46,46,46,
            46,46,46,31,31,31,31,31,31,31,
            31,31,31,31,31,31,31,31,31,31,
            31,31,31,31,31,31,31,31,31,31,
            31,31,31,31,33,33,33,33,33,33,
            33,33,33,33,33,33,33,33,33,33,
            33,33,33,33,33,33,33,33,33,33,
            33,33,33,33,33,45,45,45,45,45,
            45,487,21,17,110,111,112,113,114,115,
            116,117,118,119,120,121,122,123,124,125,
            126,127,128,129,130,131,132,133,134,135,
            271,565,22,666,22,44,313,486,3,4,
            324,373,24,25,293,40,39,110,111,112,
            113,114,115,116,117,118,119,120,121,122,
            123,124,125,126,127,128,129,130,131,132,
            133,134,135,578,679,652,37,43,547,547,
            547,547,547,547,677,547,547,547,420,390,
            31,30,110,111,112,113,114,115,116,117,
            118,119,120,121,122,123,124,125,126,127,
            128,129,130,131,132,133,134,135,45,28,
            34,547,547,547,547,547,547,547,547,547,
            547,547,454,1,236,235,110,111,112,113,
            114,115,116,117,118,119,120,121,122,123,
            124,125,126,127,128,129,130,131,132,133,
            134,135,547,547,547,547,547,547,547,547,
            547,547,547,547,547,547,547,547,47,237,
            99,40,39,110,111,112,113,114,115,116,
            117,118,119,120,121,122,123,124,125,126,
            127,128,129,130,131,132,133,134,135,547,
            547,547,38,43,196,31,30,110,111,112,
            113,114,115,116,117,118,119,120,121,122,
            123,124,125,126,127,128,129,130,131,132,
            133,134,135,547,29,34,587,19,18,110,
            111,112,113,114,115,116,117,118,119,120,
            121,122,123,124,125,126,127,128,129,130,
            131,132,133,134,135,655,21,547,547,547,
            547,547,547,547,547,547,547,547,547,547,
            547,547,547,547,547,547,547,547,547,547,
            547,547,547,547,273,547,547
        };
    };
    public final static char baseAction[] = BaseAction.baseAction;
    public final int baseAction(int index) { return baseAction[index]; }
    public final static char lhs[] = baseAction;
    public final int lhs(int index) { return lhs[index]; };

    public interface TermCheck {
        public final static byte termCheck[] = {0,
            0,1,2,3,4,5,6,7,8,9,
            10,11,12,13,14,15,16,17,18,19,
            20,21,22,23,24,25,26,27,28,29,
            30,31,32,33,34,35,36,37,38,39,
            40,41,42,43,44,45,46,47,48,49,
            50,51,52,53,54,55,56,57,58,59,
            60,61,62,63,64,65,66,67,68,69,
            70,71,72,73,74,75,76,77,78,79,
            80,81,82,83,84,85,86,87,88,89,
            90,91,92,93,94,95,96,97,0,1,
            2,3,4,5,6,7,8,9,10,11,
            12,13,14,15,16,17,18,19,20,21,
            22,23,24,25,26,27,28,29,30,31,
            32,33,34,35,36,37,38,39,40,41,
            42,43,44,45,46,47,48,49,50,51,
            52,53,54,55,56,57,58,59,60,61,
            62,63,64,65,66,67,68,69,70,71,
            72,73,74,75,76,77,78,79,80,81,
            82,83,84,85,86,87,88,89,90,91,
            92,93,94,95,96,0,1,2,3,4,
            5,6,7,8,9,10,11,12,13,14,
            15,16,17,18,19,20,21,22,23,24,
            25,26,27,28,29,30,31,32,33,34,
            35,36,37,38,39,40,41,42,43,44,
            45,46,47,48,49,50,51,52,53,54,
            55,56,57,58,59,60,61,62,63,64,
            65,66,67,68,69,70,71,72,73,74,
            75,76,77,78,79,80,81,82,83,84,
            85,86,87,88,89,90,91,92,93,94,
            95,96,0,1,2,3,4,5,6,7,
            8,9,10,11,12,13,14,15,16,17,
            18,19,20,21,22,23,24,25,26,27,
            28,29,30,31,32,33,34,35,36,37,
            38,39,40,41,42,43,44,45,46,47,
            48,49,50,51,52,53,54,55,56,57,
            58,59,60,61,62,63,64,65,66,67,
            68,69,70,71,72,73,74,75,76,0,
            78,79,80,81,82,83,84,85,86,87,
            88,89,90,91,92,93,94,95,96,0,
            1,2,3,4,5,6,7,8,9,10,
            11,12,13,14,15,16,17,18,19,20,
            21,22,23,24,25,26,27,28,29,30,
            31,32,33,34,35,36,37,38,39,40,
            41,42,43,44,45,46,47,48,49,50,
            51,52,53,54,55,56,57,58,59,60,
            61,62,63,64,65,66,67,68,69,70,
            71,72,73,74,75,0,77,78,79,80,
            81,82,83,84,85,86,87,88,89,90,
            91,92,93,94,95,96,0,1,2,3,
            4,5,6,7,8,9,10,11,12,13,
            14,15,16,17,18,19,20,21,22,23,
            24,25,26,27,28,29,30,31,32,33,
            34,35,36,37,38,39,40,41,42,43,
            44,45,46,47,48,49,50,51,52,53,
            54,55,56,57,58,59,60,61,62,63,
            64,65,66,0,68,69,70,71,72,73,
            74,75,76,77,0,1,2,3,4,5,
            6,7,8,9,10,11,0,0,0,0,
            0,0,0,97,98,99,0,1,2,3,
            4,5,6,7,8,9,10,0,0,13,
            14,15,16,17,18,19,20,21,22,23,
            24,25,26,27,28,29,30,31,32,33,
            34,35,36,37,38,39,40,41,42,43,
            44,45,46,47,48,49,50,51,52,53,
            54,55,56,57,58,59,60,61,62,63,
            64,0,0,67,0,1,2,3,4,5,
            6,7,8,9,10,0,1,2,3,4,
            5,6,7,8,9,10,0,100,0,0,
            0,0,0,0,0,0,0,0,12,11,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,66,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,65,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,0,0,0,0,0,0,0,
            0,0,0,97,98,99,0,0,0,0,
            0
        };
    };
    public final static byte termCheck[] = TermCheck.termCheck;
    public final int termCheck(int index) { return termCheck[index]; }

    public interface TermAction {
        public final static char termAction[] = {0,
            6,595,596,597,598,599,600,601,602,603,
            604,697,786,605,607,609,611,613,615,617,
            619,621,623,625,627,629,631,633,635,637,
            639,641,643,645,647,649,651,653,655,606,
            608,610,612,614,616,618,620,622,624,626,
            628,630,632,634,636,638,640,642,644,646,
            648,650,652,654,656,785,718,717,702,706,
            707,711,688,689,690,691,692,703,698,705,
            693,694,695,696,715,719,699,700,701,704,
            708,709,710,712,713,716,714,787,547,595,
            596,597,598,599,600,601,602,603,604,759,
            589,605,607,609,611,613,615,617,619,621,
            623,625,627,629,631,633,635,637,639,641,
            643,645,647,649,651,653,655,606,608,610,
            612,614,616,618,620,622,624,626,628,630,
            632,634,636,638,640,642,644,646,648,650,
            652,654,656,588,780,779,764,768,769,773,
            751,752,753,754,765,582,760,767,755,756,
            757,758,777,781,761,762,763,766,770,771,
            772,774,775,778,776,547,595,596,597,598,
            599,600,601,602,603,604,728,580,605,607,
            609,611,613,615,617,619,621,623,625,627,
            629,631,633,635,637,639,641,643,645,647,
            649,651,653,655,606,608,610,612,614,616,
            618,620,622,624,626,628,630,632,634,636,
            638,640,642,644,646,648,650,652,654,656,
            579,749,748,733,737,738,742,720,721,722,
            723,573,734,729,736,724,725,726,727,746,
            750,730,731,732,735,739,740,741,743,744,
            747,745,36,595,596,597,598,599,600,601,
            602,603,604,759,589,605,607,609,611,613,
            615,617,619,621,623,625,627,629,631,633,
            635,637,639,641,643,645,647,649,651,653,
            655,606,608,610,612,614,616,618,620,622,
            624,626,628,630,632,634,636,638,640,642,
            644,646,648,650,652,654,656,588,780,779,
            764,768,769,773,751,752,753,754,765,547,
            760,767,755,756,757,758,777,781,761,762,
            763,766,770,771,772,774,775,778,776,27,
            595,596,597,598,599,600,601,602,603,604,
            728,580,605,607,609,611,613,615,617,619,
            621,623,625,627,629,631,633,635,637,639,
            641,643,645,647,649,651,653,655,606,608,
            610,612,614,616,618,620,622,624,626,628,
            630,632,634,636,638,640,642,644,646,648,
            650,652,654,656,579,749,748,733,737,738,
            742,720,721,722,723,547,734,729,736,724,
            725,726,727,746,750,730,731,732,735,739,
            740,741,743,744,747,745,547,595,596,597,
            598,599,600,601,602,603,604,314,686,605,
            607,609,611,613,615,617,619,621,623,625,
            627,629,631,633,635,637,639,641,643,645,
            647,649,651,653,655,606,608,610,612,614,
            616,618,620,622,624,626,628,630,632,634,
            636,638,640,642,644,646,648,650,652,654,
            656,683,315,547,554,560,561,555,556,557,
            558,559,329,284,2,595,596,597,598,599,
            600,601,602,603,604,515,547,547,547,547,
            547,547,547,687,684,685,1,595,596,597,
            598,599,600,601,602,603,604,547,547,605,
            607,609,611,613,615,617,619,621,623,625,
            627,629,631,633,635,637,639,641,643,645,
            647,649,651,653,655,606,608,610,612,614,
            616,618,620,622,624,626,628,630,632,634,
            636,638,640,642,644,646,648,650,652,654,
            656,547,547,567,547,595,596,597,598,599,
            600,601,602,603,604,23,595,596,597,598,
            599,600,601,602,603,604,5,546,15,547,
            547,547,547,547,547,547,547,547,686,563,
            547,547,547,547,547,547,547,547,547,547,
            547,547,547,547,547,547,547,547,547,547,
            547,547,547,547,547,547,547,593,547,547,
            547,547,547,547,547,547,547,547,547,547,
            547,547,547,547,547,547,547,547,547,547,
            547,683,547,547,547,547,547,547,547,547,
            547,547,547,547,547,547,547,547,547,547,
            547,547,547,547,547,547,547,547,547,547,
            547,547,547,687,684,685
        };
    };
    public final static char termAction[] = TermAction.termAction;
    public final int termAction(int index) { return termAction[index]; }
    public final int asb(int index) { return 0; }
    public final int asr(int index) { return 0; }
    public final int nasb(int index) { return 0; }
    public final int nasr(int index) { return 0; }
    public final int terminalIndex(int index) { return 0; }
    public final int nonterminalIndex(int index) { return 0; }
    public final int scopePrefix(int index) { return 0;}
    public final int scopeSuffix(int index) { return 0;}
    public final int scopeLhs(int index) { return 0;}
    public final int scopeLa(int index) { return 0;}
    public final int scopeStateSet(int index) { return 0;}
    public final int scopeRhs(int index) { return 0;}
    public final int scopeState(int index) { return 0;}
    public final int inSymb(int index) { return 0;}
    public final String name(int index) { return null; }
    public final int originalState(int state) { return 0; }
    public final int asi(int state) { return 0; }
    public final int nasi(int state) { return 0; }
    public final int inSymbol(int state) { return 0; }

    /**
     * assert(! goto_default);
     */
    public final int ntAction(int state, int sym) {
        return baseAction[state + sym];
    }

    /**
     * assert(! shift_default);
     */
    public final int tAction(int state, int sym) {
        int i = baseAction[state],
            k = i + sym;
        return termAction[termCheck[k] == sym ? k : i];
    }
    public final int lookAhead(int la_state, int sym) {
        int k = la_state + sym;
        return termAction[termCheck[k] == sym ? k : la_state];
    }
}
