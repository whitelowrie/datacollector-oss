/*
 * Copyright 2019 StreamSets Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.upgrader;

import com.streamsets.pipeline.api.Config;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.StageUpgrader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class SelectorStageUpgrader implements StageUpgrader {
  private static final Logger LOG = LoggerFactory.getLogger(SelectorStageUpgrader.class);

  private StageUpgrader legacyUpgrader;
  private YamlStageUpgrader yamlUpgrader;

  public SelectorStageUpgrader(String stageName, StageUpgrader legacyUpgrader, String yamlResource) {
    this.legacyUpgrader = legacyUpgrader;
    if (yamlResource != null && !yamlResource.isEmpty()) {
      this.yamlUpgrader = new YamlStageUpgraderLoader(stageName, yamlResource).get();
    }
  }

  public List<Config> upgrade(
      String library,
      String stageName,
      String stageInstance,
      int fromVersion,
      int toVersion,
      List<Config> configs
  ) throws StageException {
    if (legacyUpgrader != null) {
      int toLegacyVersion = (yamlUpgrader != null) ? yamlUpgrader.getMinimumConfigVersionHandled() : toVersion;

      if (toLegacyVersion == YamlStageUpgrader.NO_CONFIG_VERSIONS_HANDLED) {
        LOG.debug(
            "Running legacy upgrader to upgrade '{}' stage from '{}' version to '{}' version",
            stageName,
            fromVersion,
            toVersion
        );
        configs = legacyUpgrader.upgrade(library, stageName, stageInstance, fromVersion, toVersion, configs);
      } else if (toLegacyVersion > fromVersion) {
        LOG.debug(
            "Running legacy upgrader to upgrade '{}' stage from '{}' version to '{}' version",
            stageName,
            fromVersion,
            toLegacyVersion
        );
        configs = legacyUpgrader.upgrade(library, stageName, stageInstance, fromVersion, toLegacyVersion, configs);
        fromVersion = toLegacyVersion;
      }
    }

    if (yamlUpgrader != null) {
      configs = yamlUpgrader.upgrade(library, stageName, stageInstance, fromVersion, toVersion, configs);
    }

    return configs;
  }

}
