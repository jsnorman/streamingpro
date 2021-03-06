package tech.mlsql.dsl

import org.apache.spark.sql.SparkSession
import streaming.dsl.{ScriptSQLExec, ScriptSQLExecListener}

/**
  * 2019-04-11 WilliamZhu(allwefantasy@gmail.com)
  */
object CommandCollection {
  def fill(context: ScriptSQLExecListener): Unit = {
    context.addEnv("desc", """run command as ShowTableExt.`{}`""")
    context.addEnv("kill", """run command as Kill.`{}`""")
    context.addEnv("jdbc", """ run command as JDBC.`{}` where `driver-statement-0`='''{}''' """)

    context.addEnv("cache", """ run {} as CacheExt.`` where lifeTime="{}" """)
    context.addEnv("unCache", """ run {} as CacheExt.`` where execute="uncache" """)
    context.addEnv("uncache", """ run {} as CacheExt.`` where execute="uncache" """)

    context.addEnv("createPythonEnv", """ run command as PythonEnvExt.`{}` where condaFile="{}" and command="create"  """)
    context.addEnv("removePythonEnv", """ run command as PythonEnvExt.`{}` where condaFile="{}" and command="remove" """)

    context.addEnv("createPythonEnvFromFile", """ run command as PythonEnvExt.`{}` where condaYamlFilePath="${HOME}/{}" and command="create"  """)
    context.addEnv("removePythonEnvFromFile", """ run command as PythonEnvExt.`{}` where condaYamlFilePath="${HOME}/{}" and command="remove" """)

    context.addEnv("resource",""" run command as EngineResource.`` where action="{0}" and cpus="{1}" """)

    context.addEnv("model",""" run command as ModelCommand.`{1}` where action="{0}" """)

    context.addEnv("hdfs",""" run command as HDFSCommand.`` where parameters='''{:all}''' """)
    context.addEnv("fs",""" run command as HDFSCommand.`` where parameters='''{:all}''' """)

    context.addEnv("split",""" run {0} as RateSampler.`` where labelCol="{2}" and sampleRate="{4}" as {6} """)

    context.addEnv("saveUploadFileToHome",""" run command as DownloadExt.`` where from="{}" and to="{}" """)

    context.addEnv("show",
      """
        |run command as ShowCommand.`{}/{}/{}/{}/{}/{}/{}/{}/{}/{}/{}/{}`
      """.stripMargin)
  }

  def evaluateMLSQL(spark: SparkSession, mlsql: String) = {
    val context = new ScriptSQLExecListener(spark, null, null)
    ScriptSQLExec.parse(mlsql, context, true, true)
    spark.table(context.getLastSelectTable().get)
  }
}
