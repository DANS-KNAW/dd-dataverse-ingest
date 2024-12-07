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
package nl.knaw.dans.dvingest.core.dansbag.mapper;

public interface DepositDatasetFieldNames {
    String TITLE = "title";
    String SUBTITLE = "subtitle";
    String ALTERNATIVE_TITLE = "alternativeTitle";
    String ALTERNATIVE_URL = "alternativeURL";
    String OTHER_ID = "otherId";
    String OTHER_ID_AGENCY = "otherIdAgency";
    String OTHER_ID_VALUE = "otherIdValue";
    String AUTHOR = "author";
    String AUTHOR_NAME = "authorName";
    String AUTHOR_AFFILIATION = "authorAffiliation";
    String AUTHOR_IDENTIFIER_SCHEME = "authorIdentifierScheme";
    String AUTHOR_IDENTIFIER = "authorIdentifier";
    String DATASET_CONTACT = "datasetContact";
    String DATASET_CONTACT_NAME = "datasetContactName";
    String DATASET_CONTACT_AFFILIATION = "datasetContactAffiliation";
    String DATASET_CONTACT_EMAIL = "datasetContactEmail";
    String DESCRIPTION = "dsDescription";
    String DESCRIPTION_VALUE = "dsDescriptionValue";
    String DESCRIPTION_DATE = "dsDescriptionDate";
    String SUBJECT = "subject";
    String KEYWORD = "keyword";
    String KEYWORD_VALUE = "keywordValue";
    String KEYWORD_VOCABULARY = "keywordVocabulary";
    String KEYWORD_VOCABULARY_URI = "keywordVocabularyURI";
    String TOPIC_CLASSIFICATION = "topicClassification";
    String TOPIC_CLASSVALUE = "topicClassValue";
    String TOPIC_CLASSVOCAB = "topicClassVocab";
    String TOPIC_CLASSVOCAB_URI = "topicClassVocabURI";
    String PUBLICATION = "publication";
    String PUBLICATION_CITATION = "publicationCitation";
    String PUBLICATION_ID_TYPE = "publicationIDType";
    String PUBLICATION_ID_NUMBER = "publicationIDNumber";
    String PUBLICATION_URL = "publicationURL";
    String NOTES_TEXT = "notesText";
    String LANGUAGE = "language";
    String PRODUCER = "producer";
    String PRODUCER_NAME = "producerName";
    String PRODUCER_AFFILIATION = "producerAffiliation";
    String PRODUCER_ABBREVIATION = "producerAbbreviation";
    String PRODUCER_URL = "producerURL";
    String PRODUCER_LOGO_URL = "producerLogoURL";
    String PRODUCTION_DATE = "productionDate";
    String PRODUCTION_PLACE = "productionPlace";
    String CONTRIBUTOR = "contributor";
    String CONTRIBUTOR_TYPE = "contributorType";
    String CONTRIBUTOR_NAME = "contributorName";
    String GRANT_NUMBER = "grantNumber";
    String GRANT_NUMBER_AGENCY = "grantNumberAgency";
    String GRANT_NUMBER_VALUE = "grantNumberValue";
    String DISTRIBUTOR = "distributor";
    String DISTRIBUTOR_NAME = "distributorName";
    String DISTRIBUTOR_AFFILIATION = "distributorAffiliation";
    String DISTRIBUTOR_ABBREVIATION = "distributorAbbreviation";
    String DISTRIBUTOR_URL = "distributorURL";
    String DISTRIBUTOR_LOGO_URL = "distributorLogoURL";
    String DISTRIBUTION_DATE = "distributionDate";
    String DEPOSITOR = "depositor";
    String DATE_OF_DEPOSIT = "dateOfDeposit";
    String TIME_PERIOD_COVERED = "timePeriodCovered";
    String TIME_PERIOD_COVERED_START = "timePeriodCoveredStart";
    String TIME_PERIOD_COVERED_END = "timePeriodCoveredEnd";
    String DATE_OF_COLLECTION = "dateOfCollection";
    String DATE_OF_COLLECTION_START = "dateOfCollectionStart";
    String DATE_OF_COLLECTION_END = "dateOfCollectionEnd";
    String KIND_OF_DATA = "kindOfData";
    String SERIES = "series";
    String SERIES_NAME = "seriesName";
    String SERIES_INFORMATION = "seriesInformation";
    String SOFTWARE = "software";
    String SOFTWARE_NAME = "softwareName";
    String SOFTWARE_VERSION = "softwareVersion";
    String RELATED_MATERIAL = "relatedMaterial";
    String RELATED_DATASETS = "relatedDatasets";
    String OTHER_REFERENCES = "otherReferences";
    String DATA_SOURCES = "dataSources";
    String ORIGIN_OF_SOURCES = "originOfSources";
    String CHARACTERISTICS_OF_SOURCES = "characteristicOfSources";
    String ACCESS_TO_SOURCES = "accessToSources";

    String RIGHTS_HOLDER = "dansRightsHolder";
    String PERSONAL_DATA_PRESENT = "dansPersonalDataPresent";
    String LANGUAGE_OF_METADATA = "dansMetadataLanguage";
    String AUDIENCE = "dansAudience";

    String COLLECTION = "dansCollection";

    String RELATION = "dansRelation";
    String RELATION_TYPE = "dansRelationType";
    String RELATION_URI = "dansRelationURI";
    String RELATION_TEXT = "dansRelationText";

    String ARCHIS_ZAAK_ID = "dansArchisZaakId";
    String ARCHIS_NUMBER = "dansArchisNumber";
    String ARCHIS_NUMBER_TYPE = "dansArchisNumberType";
    String ARCHIS_NUMBER_ID = "dansArchisNumberId";
    String ABR_RAPPORT_TYPE = "dansAbrRapportType";
    String ABR_RAPPORT_NUMMER = "dansAbrRapportNummer";
    String ABR_VERWERVINGSWIJZE = "dansAbrVerwervingswijze";
    String ABR_COMPLEX = "dansAbrComplex";
    String ABR_ARTIFACT = "dansAbrArtifact";
    String ABR_PERIOD = "dansAbrPeriod";

    String ABR_BASE_URL = "https://data.cultureelerfgoed.nl/term/id/abr";
    String SCHEME_ABR_OLD = "Archeologisch Basis Register";
    String SCHEME_URI_ABR_OLD = "https://data.cultureelerfgoed.nl/term/id/rn/a4a7933c-e096-4bcf-a921-4f70a78749fe";
    String SCHEME_ABR_PLUS = "Archeologisch Basis Register";
    String SCHEME_URI_ABR_PLUS = "https://data.cultureelerfgoed.nl/term/id/abr/b6df7840-67bf-48bd-aa56-7ee39435d2ed";

    String SCHEME_ABR_COMPLEX = "ABR Complextypen";
    String SCHEME_URI_ABR_COMPLEX = "https://data.cultureelerfgoed.nl/term/id/abr/e9546020-4b28-4819-b0c2-29e7c864c5c0";

    String SCHEME_ABR_ARTIFACT = "ABR Artefacten";
    String SCHEME_URI_ABR_ARTIFACT = "https://data.cultureelerfgoed.nl/term/id/abr/22cbb070-6542-48f0-8afe-7d98d398cc0b";

    String SCHEME_ABR_PERIOD = "ABR Periodes";
    String SCHEME_URI_ABR_PERIOD = "https://data.cultureelerfgoed.nl/term/id/abr/9b688754-1315-484b-9c89-8817e87c1e84";

    String SCHEME_ABR_RAPPORT_TYPE = "ABR Rapporten";
    String SCHEME_URI_ABR_RAPPORT_TYPE = "https://data.cultureelerfgoed.nl/term/id/abr/7a99aaba-c1e7-49a4-9dd8-d295dbcc870e";

    String SCHEME_ABR_VERWERVINGSWIJZE = "ABR verwervingswijzen";
    String SCHEME_URI_ABR_VERWERVINGSWIJZE = "https://data.cultureelerfgoed.nl/term/id/abr/554ca1ec-3ed8-42d3-ae4b-47bcb848b238";

    String TEMPORAL_COVERAGE = "dansTemporalCoverage";
    String SPATIAL_POINT = "dansSpatialPoint";
    String SPATIAL_POINT_SCHEME = "dansSpatialPointScheme";
    String SPATIAL_POINT_X = "dansSpatialPointX";
    String SPATIAL_POINT_Y = "dansSpatialPointY";
    String SPATIAL_BOX = "dansSpatialBox";
    String SPATIAL_BOX_SCHEME = "dansSpatialBoxScheme";
    String SPATIAL_BOX_NORTH = "dansSpatialBoxNorth";
    String SPATIAL_BOX_EAST = "dansSpatialBoxEast";
    String SPATIAL_BOX_SOUTH = "dansSpatialBoxSouth";
    String SPATIAL_BOX_WEST = "dansSpatialBoxWest";
    String SPATIAL_COVERAGE_CONTROLLED = "dansSpatialCoverageControlled";
    String SPATIAL_COVERAGE_UNCONTROLLED = "dansSpatialCoverageText";
    String DATAVERSE_PID = "dansDataversePid";
    String DATAVERSE_PID_VERSION = "dansDataversePidVersion";
    String BAG_ID = "dansBagId";
    String NBN = "dansNbn";
    String DANS_OTHER_ID = "dansOtherId";
    String DANS_OTHER_ID_VERSION = "dansOtherIdVersion";
    String SWORD_TOKEN = "dansSwordToken";
    String DATA_SUPPLIER = "dansDataSupplier";
}
