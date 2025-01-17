/*
 * Copyright (C) 2024 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Classes for reading and writing the action log in YAML format. The action log keeps track of the actions that have been performed on a dataset. It can be used to resume an ingest process after a
 * failure. Optional steps that are absent are assumed to have been completed successfully when the processor reaches them.
 */
package nl.knaw.dans.dvingest.core.yaml.actionlog;