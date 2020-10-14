package pdfbox.sample;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

public class ColorTextStripper extends PDFTextStripper {

  static final List<RenderingMode> FILLING_MODES =
      Arrays.asList(
          RenderingMode.FILL,
          RenderingMode.FILL_STROKE,
          RenderingMode.FILL_CLIP,
          RenderingMode.FILL_STROKE_CLIP);

  static final List<RenderingMode> STROKING_MODES =
      Arrays.asList(
          RenderingMode.STROKE,
          RenderingMode.FILL_STROKE,
          RenderingMode.STROKE_CLIP,
          RenderingMode.FILL_STROKE_CLIP);

  static final List<RenderingMode> CLIPPING_MODES =
      Arrays.asList(
          RenderingMode.FILL_CLIP,
          RenderingMode.STROKE_CLIP,
          RenderingMode.FILL_STROKE_CLIP,
          RenderingMode.NEITHER_CLIP);

  Map<TextPosition, RenderingMode> renderingMode = new HashMap<>();
  Map<TextPosition, float[]> strokingColor = new HashMap<>();
  Map<TextPosition, float[]> nonStrokingColor = new HashMap<>();

  public ColorTextStripper() throws IOException {
    super();
    setSuppressDuplicateOverlappingText(false);

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

  @Override
  protected void processTextPosition(TextPosition text) {
    renderingMode.put(text, getGraphicsState().getTextState().getRenderingMode());
    strokingColor.put(text, getGraphicsState().getStrokingColor().getComponents());
    nonStrokingColor.put(text, getGraphicsState().getNonStrokingColor().getComponents());

    super.processTextPosition(text);
  }

  @Override
  protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
    for (TextPosition textPosition : textPositions) {
      RenderingMode charRenderingMode = renderingMode.get(textPosition);
      float[] charStrokingColor = strokingColor.get(textPosition);
      float[] charNonStrokingColor = nonStrokingColor.get(textPosition);

      StringBuilder textBuilder = new StringBuilder();
      textBuilder.append(textPosition.getUnicode()).append("{");

      if (FILLING_MODES.contains(charRenderingMode)) {
        textBuilder.append("FILL:").append(toString(charNonStrokingColor)).append(';');
      }

      if (STROKING_MODES.contains(charRenderingMode)) {
        textBuilder.append("STROKE:").append(toString(charStrokingColor)).append(';');
      }

      if (CLIPPING_MODES.contains(charRenderingMode)) {
        textBuilder.append("CLIP;");
      }

      textBuilder.append("}");
      writeString(textBuilder.toString());
    }
  }

  String toString(float[] values) {
    if (values == null) return "null";
    StringBuilder builder = new StringBuilder();
    switch (values.length) {
      case 1:
        builder.append("GRAY");
        break;
      case 3:
        builder.append("RGB");
        break;
      case 4:
        builder.append("CMYK");
        break;
      default:
        builder.append("UNKNOWN");
    }
    for (float f : values) {
      builder.append(' ').append(f);
    }

    return builder.toString();
  }

  public static void processPdf(byte[] input) throws IOException {
    try (PDDocument document = PDDocument.load(input)) {
      PDFTextStripper stripper = new ColorTextStripper();
      stripper.setSortByPosition(true);
      stripper.setStartPage(0);
      stripper.setEndPage(document.getNumberOfPages());

      String text = stripper.getText(document);
      System.out.println(text);
    }
  }
}
