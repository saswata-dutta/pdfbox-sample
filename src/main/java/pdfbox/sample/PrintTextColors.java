package pdfbox.sample;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetNonStrokingDeviceRGBColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorN;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingColorSpace;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceCMYKColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceGrayColor;
import org.apache.pdfbox.contentstream.operator.color.SetStrokingDeviceRGBColor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class PrintTextColors extends PDFTextStripper {
  public PrintTextColors() throws IOException {
    addOperator(new SetStrokingColorSpace());
    addOperator(new SetNonStrokingColorSpace());
    addOperator(new SetStrokingDeviceCMYKColor());
    addOperator(new SetNonStrokingDeviceCMYKColor());
    addOperator(new SetNonStrokingDeviceRGBColor());
    addOperator(new SetStrokingDeviceRGBColor());
    addOperator(new SetNonStrokingDeviceGrayColor());
    addOperator(new SetStrokingDeviceGrayColor());
    addOperator(new SetStrokingColor());
    addOperator(new SetStrokingColorN());
    addOperator(new SetNonStrokingColor());
    addOperator(new SetNonStrokingColorN());
  }

  public static void processPdf(byte[] input) throws IOException {
    try (PDDocument document = PDDocument.load(input)) {
      PDFTextStripper stripper = new PrintTextColors();
      stripper.setSortByPosition(true);
      stripper.setStartPage(0);
      stripper.setEndPage(document.getNumberOfPages());

      Writer dummy = new OutputStreamWriter(new ByteArrayOutputStream());
      stripper.writeText(document, dummy);
    }
  }

  @Override
  protected void writeString(String string, List<TextPosition> textPositions) throws IOException {
    super.writeString(string, textPositions);
    System.out.println(string);
    printTextPosition(textPositions.get(0));
  }

  private void printTextPosition(TextPosition text) {

    PDGraphicsState graphicsState = getGraphicsState();
    PDColor strokingColor = graphicsState.getStrokingColor();
    PDColor nonStrokingColor = graphicsState.getNonStrokingColor();
    RenderingMode renderingMode = graphicsState.getTextState().getRenderingMode();
    String unicode = text.getUnicode();

    System.out.println("1st char Unicode:            " + unicode);
    System.out.println("Rendering mode:     " + renderingMode);
    System.out.println("Stroking color:     " + strokingColor);
    System.out.println("Non-Stroking color: " + nonStrokingColor);
    System.out.println(
        "String["
            + text.getXDirAdj()
            + ","
            + text.getYDirAdj()
            + " fs="
            + text.getFontSize()
            + " xscale="
            + text.getXScale()
            + " height="
            + text.getHeightDir()
            + " space="
            + text.getWidthOfSpace()
            + " width="
            + text.getWidthDirAdj()
            + "]");
    System.out.println();
  }
}
