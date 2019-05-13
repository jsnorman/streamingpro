/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package streaming.dsl.mmlib.algs.python

import org.apache.spark.sql.SparkSession
import streaming.common.PathFun
import streaming.dsl.mmlib.algs.MetaConst
import streaming.dsl.mmlib.algs.SQLPythonFunc._
import streaming.log.{Logging, WowLog}

class ModelMetaManager(sparkSession: SparkSession, _path: String, params: Map[String, String]) extends Logging with WowLog {

  val wowMetas = sparkSession.read.parquet(metaPath + "/1").collect()
  
  val _trainParams = trainParams
  val isAutoGenerate = _trainParams.contains("entryPoint") && _trainParams.contains("scripts")



  def loadMetaAndModel(localPathConfig: LocalPathConfig, modelHDFSToLocalPath: Map[String, String]) = {
    val autoGenerateDir = PathFun(_path).add("mlsql-python-project").toPath
    val loadProjectParams = if (isAutoGenerate) {
      _trainParams ++ Map(
        "pythonScriptPath" -> autoGenerateDir,
        "pythonDescPath" -> autoGenerateDir
      )
    } else _trainParams

    val pythonTrainScript = PythonAlgProject.loadProject(loadProjectParams, sparkSession)
    ModelMeta(pythonTrainScript.get, loadProjectParams, modelEntityPaths, Map(), localPathConfig, modelHDFSToLocalPath)
  }

  def maxVersion = getModelVersion(_path)

  def versionEnabled = maxVersion match {
    case Some(v) => true
    case None => false
  }

  def modelVersion = params.getOrElse("modelVersion", maxVersion.getOrElse(-1).toString).toInt

  def metaPath = {
    if (modelVersion == -1) getAlgMetalPath(_path, versionEnabled)
    else getAlgMetalPathWithVersion(_path, modelVersion)
  }

  def modelPath = if (modelVersion == -1) getAlgModelPath(_path, versionEnabled)
  else getAlgModelPathWithVersion(_path, modelVersion)

  def modelEntityPaths = {
    var algIndex = params.getOrElse("algIndex", "-1").toInt
    val modelList = sparkSession.read.parquet(metaPath + "/0").collect()
    val models = if (algIndex != -1) {
      Seq(modelPath + "/" + algIndex)
    } else {
      modelList.map(f => (f(3).asInstanceOf[Double], f(0).asInstanceOf[String], f(1).asInstanceOf[Int]))
        .toSeq
        .sortBy(f => f._1)(Ordering[Double].reverse)
        .take(1)
        .map(f => {
          algIndex = f._3
          modelPath + "/" + f._2.split("/").last
        })
    }
    models
  }

  def trainParams = {

    import sparkSession.implicits._

    var trainParams = Map[String, String]()

    def getTrainParams(isNew: Boolean) = {
      if (isNew)
        wowMetas.map(f => f.getMap[String, String](1)).head.toMap
      else {
        val df = sparkSession.read.parquet(MetaConst.PARAMS_PATH(metaPath, "params")).map(f => (f.getString(0), f.getString(1)))
        df.collect().toMap
      }
    }

    if (versionEnabled) {
      trainParams = getTrainParams(true)
    }

    try {
      trainParams = getTrainParams(false)
    } catch {
      case e: Exception =>
        logInfo(format(s"no directory: ${MetaConst.PARAMS_PATH(metaPath, "params")} ; using ${metaPath + "/1"}"))
        trainParams = getTrainParams(true)
    }
    trainParams
  }


}
