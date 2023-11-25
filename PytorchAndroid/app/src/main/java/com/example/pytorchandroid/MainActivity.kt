package com.example.pytorchandroid

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pytorchandroid.ui.theme.PytorchAndroidTheme
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Tensor
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import com.example.pytorchandroid.ImageNetLabels.ImageNetLabels
import java.nio.ByteBuffer
import java.nio.FloatBuffer


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PytorchAndroidTheme {
                val context = applicationContext
                PytorchAndroidApp(context)
            }
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

@Composable
fun ImageField(
    bitmap: ImageBitmap,
    context: Context,
    modifier: Modifier = Modifier) {

    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Crop)
}

@Composable
fun PredictionTextField(
    score: Float,
    label: String,
    modifier: Modifier) {
    Column (
        modifier = modifier) {
        Text(text="Prediction: $label")
        Text(text="Confidence: $score" )
    }

}

fun assetFilePath(context: Context, asset: String): String {
    val file = File(context.filesDir, asset)

    try {
        val inpStream: InputStream = context.assets.open(asset)
        try {
            val outStream = FileOutputStream(file, false)
            val buffer = ByteArray(4 * 1024)
            var read: Int

            while (true) {
                read = inpStream.read(buffer)
                if (read == -1) {
                    break
                }
                outStream.write(buffer, 0, read)
            }
            outStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ""
}


@Composable
fun PytorchAndroidApp(
    context: Context,
    modifier: Modifier = Modifier) {

    val padding = 16.dp
    val inputStream: InputStream = context.assets.open("image.jpg")
    val bitmap = BitmapFactory.decodeStream(inputStream)
    val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

    /* Load MobileNet */
    val liteModule = LiteModuleLoader.load(assetFilePath(context, "model.ptl"))
    println("lite Module: " + liteModule)

    /* Create a tensor to hold image */
    /*
    val size = 224 * 224 * 3 * 1
    val buffer = FloatArray(size) { 1f }

    val onesFloatBuffer = Tensor.allocateFloatBuffer(size)
    onesFloatBuffer.put(buffer)

    val shape: LongArray = LongArray(4, {1L})
    shape[0] = 1
    shape[1] = 3
    shape[2] = 224
    shape[3] = 224
    val resizedInputTensor: Tensor = Tensor.fromBlob(
        onesFloatBuffer,
        shape)
     */

    val resizedInputTensor: Tensor = TensorImageUtils.bitmapToFloat32Tensor(
        resizedBitmap,
        TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
        TensorImageUtils.TORCHVISION_NORM_STD_RGB,
    )

    println("Input tensor: " + resizedInputTensor)
    for (idx in 1..10) {
        println("idx: $idx, value: ${resizedInputTensor.dataAsFloatArray[idx]}")
    }

    /* Run inference */
    val outTensor: Tensor = liteModule.forward(IValue.from(resizedInputTensor)).toTensor()
    val scores: FloatArray = outTensor.getDataAsFloatArray()
    println("Output tensor: " + scores)

    var maxScore: Float = -Float.MAX_VALUE
    var maxScoreIndex: Int = 0
    for ((index, score) in scores.withIndex()) {
        if( score > maxScore ) {
            maxScore = score
            maxScoreIndex = index
        }
        val pred_i = ImageNetLabels().getLabel(index)
        println("Index: $index, Score: $score, Prediction: $pred_i")

    }
    val pred: String = ImageNetLabels().getLabel(maxScoreIndex)
    //println("Score: $maxScore, Prediction: $pred")


    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .fillMaxSize()) {
        ImageField(
            bitmap = resizedBitmap.asImageBitmap(),
            context = context,
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding))
        PredictionTextField(
            label = pred,
            score = maxScore,
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding))
    }
}

@Preview
@Composable
fun PytorchAndroidAppPreview() {
    PytorchAndroidTheme {
        val context = LocalContext.current
        PytorchAndroidApp(context = context)
    }
}