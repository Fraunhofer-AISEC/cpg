@AnnotationForClass(value = 2)
public class Annotation {

  @AnnotatedField
  private int field = 1;

  @AnnotatedField("myString")
  private int anotherField = 2;
}
