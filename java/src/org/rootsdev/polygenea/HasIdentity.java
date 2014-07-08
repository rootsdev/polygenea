package org.rootsdev.polygenea;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A node annotated with @HasIdentity will be considered distinct from other
 * nodes of the same type even if those other nodes have identical fields. This
 * distinction is realized by giving the node a type-1 or type-4 UUID instead of
 * a content-based type-5 UUID.
 * 
 * @author Luther Tychonievich. Released into the public domain. I would consider it a courtesy if you cite my contributions to any code derived from this code or project that uses this code.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface HasIdentity {}
