package sbt
package librarymanagement
package ivy

import sbt.internal.librarymanagement._
import sbt.util.Logger

class IvyDependencyResolution private[sbt] (val ivySbt: IvySbt)
    extends DependencyResolutionInterface {
  type Module = ivySbt.Module

  override def moduleDescriptor(moduleSetting: ModuleDescriptorConfiguration): ModuleDescriptor = {
    new Module(moduleSetting)
  }

  override def update(module: ModuleDescriptor,
                      configuration: UpdateConfiguration,
                      uwconfig: UnresolvedWarningConfiguration,
                      log: Logger): Either[UnresolvedWarning, UpdateReport] =
    IvyActions.updateEither(toModule(module), configuration, uwconfig, log).right.map { r =>
      log.info(s"Update report:\n$r")
      r
    }

  private[sbt] def toModule(module: ModuleDescriptor): Module =
    module match {
      case m: Module @unchecked => m
    }
}

object IvyDependencyResolution {
  def apply(ivyConfiguration: IvyConfiguration): DependencyResolution =
    DependencyResolution(new IvyDependencyResolution(new IvySbt(ivyConfiguration)))
}
