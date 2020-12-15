package de.fraunhofer.aisec.cpg.helpers;

import de.fraunhofer.aisec.cpg.BaseTest;
import org.neo4j.ogm.typeconversion.CompositeAttributeConverter;

abstract class BaseAttributeConverterTest<T> extends BaseTest {
  abstract CompositeAttributeConverter<T> getSut();
}
