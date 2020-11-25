package com.goldberg.losslessvideocutter

import com.goldberg.losslessvideocutter.Constants.KEYFRAME_TIMING_EXTRACTION_REGEX
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VideoInfoTest
{
    @Test
    fun testExtractKeyFrameTimings()
    {
        val regex = KEYFRAME_TIMING_EXTRACTION_REGEX
        val matchResults = regex.findAll(FFPROBE_KEYFRAME_INFO_OUTPUT)

        assertThat(matchResults.count()).isEqualTo(MATCH_RESULTS.size)

        matchResults.forEachIndexed { index, matchResult ->
            val groups = matchResult.groupValues
            assertThat(groups.count()).isEqualTo(2)
            assertThat(groups[1].toFloatOrNull()).isEqualTo(MATCH_RESULTS[index])
        }
    }

    companion object
    {
        private val MATCH_RESULTS = arrayOf(0.0f, 1.106544f, 2.108878f, 3.111211f, 4.113722f, 5.115978f, 6.118311f, 7.120644f, 8.122978f, 9.125411f, 10.127878f, 11.130167f)

        private const val FFPROBE_KEYFRAME_INFO_OUTPUT = "0.000000,K_\n" +
                "    0.137578,__\n" +
                "    0.170967,__\n" +
                "    0.204356,__\n" +
                "    0.237744,__\n" +
                "    0.271133,__\n" +
                "    0.304644,__\n" +
                "    0.338156,__\n" +
                "    0.371478,__\n" +
                "    0.404800,__\n" +
                "    0.438300,__\n" +
                "    0.471800,__\n" +
                "    0.505156,__\n" +
                "    0.538511,__\n" +
                "    0.571867,__\n" +
                "    0.605378,__\n" +
                "    0.638789,__\n" +
                "    0.672200,__\n" +
                "    0.705611,__\n" +
                "    0.739022,__\n" +
                "    0.772433,__\n" +
                "    0.805844,__\n" +
                "    0.839256,__\n" +
                "    0.872667,__\n" +
                "    0.906078,__\n" +
                "    0.939489,__\n" +
                "    0.972900,__\n" +
                "    1.006311,__\n" +
                "    1.039722,__\n" +
                "    1.073133,__\n" +
                "    1.106544,K_\n" +
                "    1.139956,__\n" +
                "    1.173367,__\n" +
                "    1.206778,__\n" +
                "    1.240189,__\n" +
                "    1.273600,__\n" +
                "    1.307011,__\n" +
                "    1.340422,__\n" +
                "    1.373833,__\n" +
                "    1.407244,__\n" +
                "    1.440656,__\n" +
                "    1.474067,__\n" +
                "    1.507478,__\n" +
                "    1.540889,__\n" +
                "    1.574300,__\n" +
                "    1.607711,__\n" +
                "    1.641122,__\n" +
                "    1.674533,__\n" +
                "    1.707944,__\n" +
                "    1.741356,__\n" +
                "    1.774767,__\n" +
                "    1.808178,__\n" +
                "    1.841589,__\n" +
                "    1.875000,__\n" +
                "    1.908411,__\n" +
                "    1.941822,__\n" +
                "    1.975233,__\n" +
                "    2.008644,__\n" +
                "    2.042056,__\n" +
                "    2.075467,__\n" +
                "    2.108878,K_\n" +
                "    2.142289,__\n" +
                "    2.175700,__\n" +
                "    2.209111,__\n" +
                "    2.242522,__\n" +
                "    2.275933,__\n" +
                "    2.309344,__\n" +
                "    2.342756,__\n" +
                "    2.376167,__\n" +
                "    2.409578,__\n" +
                "    2.442989,__\n" +
                "    2.476400,__\n" +
                "    2.509811,__\n" +
                "    2.543222,__\n" +
                "    2.576633,__\n" +
                "    2.610044,__\n" +
                "    2.643456,__\n" +
                "    2.676867,__\n" +
                "    2.710278,__\n" +
                "    2.743689,__\n" +
                "    2.777100,__\n" +
                "    2.810511,__\n" +
                "    2.843922,__\n" +
                "    2.877333,__\n" +
                "    2.910744,__\n" +
                "    2.944156,__\n" +
                "    2.977567,__\n" +
                "    3.010978,__\n" +
                "    3.044389,__\n" +
                "    3.077800,__\n" +
                "    3.111211,K_\n" +
                "    3.144622,__\n" +
                "    3.178033,__\n" +
                "    3.211444,__\n" +
                "    3.244856,__\n" +
                "    3.278267,__\n" +
                "    3.311678,__\n" +
                "    3.345089,__\n" +
                "    3.378500,__\n" +
                "    3.411911,__\n" +
                "    3.445322,__\n" +
                "    3.478733,__\n" +
                "    3.512256,__\n" +
                "    3.545633,__\n" +
                "    3.579011,__\n" +
                "    3.612389,__\n" +
                "    3.645767,__\n" +
                "    3.679256,__\n" +
                "    3.712744,__\n" +
                "    3.746078,__\n" +
                "    3.779411,__\n" +
                "    3.812911,__\n" +
                "    3.846411,__\n" +
                "    3.879733,__\n" +
                "    3.913056,__\n" +
                "    3.946556,__\n" +
                "    3.980056,__\n" +
                "    4.013378,__\n" +
                "    4.046700,__\n" +
                "    4.080211,__\n" +
                "    4.113722,K_\n" +
                "    4.147033,__\n" +
                "    4.180344,__\n" +
                "    4.213867,__\n" +
                "    4.247267,__\n" +
                "    4.280667,__\n" +
                "    4.314067,__\n" +
                "    4.347467,__\n" +
                "    4.380867,__\n" +
                "    4.414267,__\n" +
                "    4.447667,__\n" +
                "    4.481067,__\n" +
                "    4.514578,__\n" +
                "    4.547989,__\n" +
                "    4.581400,__\n" +
                "    4.614811,__\n" +
                "    4.648222,__\n" +
                "    4.681633,__\n" +
                "    4.715044,__\n" +
                "    4.748456,__\n" +
                "    4.781867,__\n" +
                "    4.815278,__\n" +
                "    4.848689,__\n" +
                "    4.882100,__\n" +
                "    4.915511,__\n" +
                "    4.948922,__\n" +
                "    4.982333,__\n" +
                "    5.015744,__\n" +
                "    5.049156,__\n" +
                "    5.082567,__\n" +
                "    5.115978,K_\n" +
                "    5.149389,__\n" +
                "    5.182800,__\n" +
                "    5.216211,__\n" +
                "    5.249622,__\n" +
                "    5.283033,__\n" +
                "    5.316444,__\n" +
                "    5.349856,__\n" +
                "    5.383267,__\n" +
                "    5.416678,__\n" +
                "    5.450089,__\n" +
                "    5.483500,__\n" +
                "    5.516911,__\n" +
                "    5.550322,__\n" +
                "    5.583733,__\n" +
                "    5.617144,__\n" +
                "    5.650556,__\n" +
                "    5.683967,__\n" +
                "    5.717378,__\n" +
                "    5.750789,__\n" +
                "    5.784200,__\n" +
                "    5.817611,__\n" +
                "    5.851022,__\n" +
                "    5.884433,__\n" +
                "    5.917844,__\n" +
                "    5.951256,__\n" +
                "    5.984667,__\n" +
                "    6.018078,__\n" +
                "    6.051489,__\n" +
                "    6.084900,__\n" +
                "    6.118311,K_\n" +
                "    6.151722,__\n" +
                "    6.185133,__\n" +
                "    6.218544,__\n" +
                "    6.251956,__\n" +
                "    6.285367,__\n" +
                "    6.318778,__\n" +
                "    6.352189,__\n" +
                "    6.385600,__\n" +
                "    6.419011,__\n" +
                "    6.452422,__\n" +
                "    6.485833,__\n" +
                "    6.519244,__\n" +
                "    6.552656,__\n" +
                "    6.586067,__\n" +
                "    6.619478,__\n" +
                "    6.652889,__\n" +
                "    6.686300,__\n" +
                "    6.719711,__\n" +
                "    6.753122,__\n" +
                "    6.786533,__\n" +
                "    6.819944,__\n" +
                "    6.853356,__\n" +
                "    6.886767,__\n" +
                "    6.920178,__\n" +
                "    6.953589,__\n" +
                "    6.987000,__\n" +
                "    7.020411,__\n" +
                "    7.053822,__\n" +
                "    7.087233,__\n" +
                "    7.120644,K_\n" +
                "    7.154056,__\n" +
                "    7.187467,__\n" +
                "    7.220878,__\n" +
                "    7.254289,__\n" +
                "    7.287700,__\n" +
                "    7.321111,__\n" +
                "    7.354522,__\n" +
                "    7.387933,__\n" +
                "    7.421344,__\n" +
                "    7.454756,__\n" +
                "    7.488167,__\n" +
                "    7.521578,__\n" +
                "    7.554989,__\n" +
                "    7.588400,__\n" +
                "    7.621811,__\n" +
                "    7.655222,__\n" +
                "    7.688633,__\n" +
                "    7.722044,__\n" +
                "    7.755456,__\n" +
                "    7.788867,__\n" +
                "    7.822278,__\n" +
                "    7.855689,__\n" +
                "    7.889100,__\n" +
                "    7.922511,__\n" +
                "    7.955922,__\n" +
                "    7.989333,__\n" +
                "    8.022744,__\n" +
                "    8.056156,__\n" +
                "    8.089567,__\n" +
                "    8.122978,K_\n" +
                "    8.156389,__\n" +
                "    8.189800,__\n" +
                "    8.223211,__\n" +
                "    8.256622,__\n" +
                "    8.290033,__\n" +
                "    8.323444,__\n" +
                "    8.356856,__\n" +
                "    8.390267,__\n" +
                "    8.423678,__\n" +
                "    8.457089,__\n" +
                "    8.490500,__\n" +
                "    8.523911,__\n" +
                "    8.557322,__\n" +
                "    8.590733,__\n" +
                "    8.624289,__\n" +
                "    8.657656,__\n" +
                "    8.691022,__\n" +
                "    8.724389,__\n" +
                "    8.757922,__\n" +
                "    8.791311,__\n" +
                "    8.824700,__\n" +
                "    8.858089,__\n" +
                "    8.891589,__\n" +
                "    8.924967,__\n" +
                "    8.958344,__\n" +
                "    8.991722,__\n" +
                "    9.025100,__\n" +
                "    9.058611,__\n" +
                "    9.092011,__\n" +
                "    9.125411,K_\n" +
                "    9.158811,__\n" +
                "    9.192211,__\n" +
                "    9.225611,__\n" +
                "    9.259011,__\n" +
                "    9.292411,__\n" +
                "    9.325811,__\n" +
                "    9.359322,__\n" +
                "    9.392833,__\n" +
                "    9.426144,__\n" +
                "    9.459456,__\n" +
                "    9.492956,__\n" +
                "    9.526456,__\n" +
                "    9.559811,__\n" +
                "    9.593300,__\n" +
                "    9.626622,__\n" +
                "    9.659944,__\n" +
                "    9.693467,__\n" +
                "    9.726867,__\n" +
                "    9.760267,__\n" +
                "    9.793667,__\n" +
                "    9.827067,__\n" +
                "    9.860467,__\n" +
                "    9.893867,__\n" +
                "    9.927267,__\n" +
                "    9.960822,__\n" +
                "    9.994144,__\n" +
                "    10.027578,__\n" +
                "    10.061011,__\n" +
                "    10.094444,__\n" +
                "    10.127878,K_\n" +
                "    10.161311,__\n" +
                "    10.194611,__\n" +
                "    10.228067,__\n" +
                "    10.261522,__\n" +
                "    10.294856,__\n" +
                "    10.328311,__\n" +
                "    10.361767,__\n" +
                "    10.395122,__\n" +
                "    10.428478,__\n" +
                "    10.461944,__\n" +
                "    10.495411,__\n" +
                "    10.528744,__\n" +
                "    10.562222,__\n" +
                "    10.595700,__\n" +
                "    10.628989,__\n" +
                "    10.662422,__\n" +
                "    10.695856,__\n" +
                "    10.729289,__\n" +
                "    10.762722,__\n" +
                "    10.796044,__\n" +
                "    10.829367,__\n" +
                "    10.862878,__\n" +
                "    10.896289,__\n" +
                "    10.929700,__\n" +
                "    10.963111,__\n" +
                "    10.996522,__\n" +
                "    11.029933,__\n" +
                "    11.063344,__\n" +
                "    11.096756,__\n" +
                "    11.130167,K_\n" +
                "    11.163689,__\n" +
                "    11.197000,\n" +
                "\n"
    }
}