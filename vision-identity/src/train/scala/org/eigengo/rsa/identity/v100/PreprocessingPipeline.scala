/*
 * The Reactive Summit Austin talk
 * Copyright (C) 2016 Jan Machacek
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.eigengo.rsa.identity.v100

import java.io.File
import java.util.UUID

import scala.util.{Failure, Try}

class PreprocessingPipeline(sourceDirectory: File, targetDirectory: File,
                                  preprocessors: List[Preprocessor]) {
  import org.bytedeco.javacpp.opencv_imgcodecs._

  def preprocess(): Try[Seq[File]] = {
    if (!sourceDirectory.exists()) Failure(new Exception(s"$sourceDirectory does not exist."))
    if (!targetDirectory.mkdirs()) Failure(new Exception(s"Cannot create $targetDirectory."))

    def preprocessSourceFile(preprocessors: List[Preprocessor], sourceFile: File, targetDirectory: File): List[File] = preprocessors match {
      case h::t ⇒
        val mat = imread(sourceFile.getAbsolutePath)
        h.preprocess(mat).flatMap { result ⇒
          val targetFile = new File(targetDirectory, UUID.randomUUID().toString + ".jpg")
          imwrite(targetFile.getAbsolutePath, result)
          targetFile :: preprocessSourceFile(t, targetFile, targetDirectory)
        }
      case Nil ⇒ Nil
    }

    def preprocess(source: File, targetDirectory: File): Seq[File] = {
      if (source.isDirectory) {
        val target = new File(targetDirectory, source.getName)
        if (target.exists() || target.mkdirs()) {
          source.listFiles().flatMap(x ⇒ preprocess(x, new File(targetDirectory, source.getName)))
        } else {
          Nil
        }
      } else {
        preprocessSourceFile(preprocessors, source, targetDirectory)
      }
    }

    Try(sourceDirectory.listFiles().flatMap { file ⇒
      if (file.isDirectory) preprocess(file, targetDirectory) else Nil
    })
  }

}
