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
package nl.knaw.dans.ingest.core.service;

public class DepositDatasetFieldNames {
    public final static String TITLE = "title";
    public final static String SUBTITLE = "subtitle";
    public final static String ALTERNATIVE_TITLE = "alternativeTitle";
    public final static String ALTERNATIVE_URL = "alternativeURL";
    public final static String OTHER_ID = "otherId";
    public final static String OTHER_ID_AGENCY = "otherIdAgency";
    public final static String OTHER_ID_VALUE = "otherIdValue";
    public final static String AUTHOR = "author";
    public final static String AUTHOR_NAME = "authorName";
    public final static String AUTHOR_AFFILIATION = "authorAffiliation";
    public final static String AUTHOR_IDENTIFIER_SCHEME = "authorIdentifierScheme";
    public final static String AUTHOR_IDENTIFIER = "authorIdentifier";
    public final static String DATASET_CONTACT = "datasetContact";
    public final static String DATASET_CONTACT_NAME = "datasetContactName";
    public final static String DATASET_CONTACT_AFFILIATION = "datasetContactAffiliation";
    public final static String DATASET_CONTACT_EMAIL = "datasetContactEmail";
    public final static String DESCRIPTION = "dsDescription";
    public final static String DESCRIPTION_VALUE = "dsDescriptionValue";
    public final static String DESCRIPTION_DATE = "dsDescriptionDate";
    public final static String SUBJECT = "subject";
    public final static String KEYWORD = "keyword";
    public final static String KEYWORD_VALUE = "keywordValue";
    public final static String KEYWORD_VOCABULARY = "keywordVocabulary";
    public final static String KEYWORD_VOCABULARY_URI = "keywordVocabularyURI";
    public final static String TOPIC_CLASSIFICATION = "topicClassification";
    public final static String TOPIC_CLASSVALUE = "topicClassValue";
    public final static String TOPIC_CLASSVOCAB = "topicClassVocab";
    public final static String TOPIC_CLASSVOCAB_URI = "topicClassVocabURI";
    public final static String PUBLICATION = "publication";
    public final static String PUBLICATION_CITATION = "publicationCitation";
    public final static String PUBLICATION_ID_TYPE = "publicationIDType";
    public final static String PUBLICATION_ID_NUMBER = "publicationIDNumber";
    public final static String PUBLICATION_URL = "publicationURL";
    public final static String NOTES_TEXT = "notesText";
    public final static String LANGUAGE = "language";
    public final static String PRODUCER = "producer";
    public final static String PRODUCER_NAME = "producerName";
    public final static String PRODUCER_AFFILIATION = "producerAffiliation";
    public final static String PRODUCER_ABBREVIATION = "producerAbbreviation";
    public final static String PRODUCER_URL = "producerURL";
    public final static String PRODUCER_LOGO_URL = "producerLogoURL";
    public final static String PRODUCTION_DATE = "productionDate";
    public final static String PRODUCTION_PLACE = "productionPlace";
    public final static String CONTRIBUTOR = "contributor";
    public final static String CONTRIBUTOR_TYPE = "contributorType";
    public final static String CONTRIBUTOR_NAME = "contributorName";
    public final static String GRANT_NUMBER = "grantNumber";
    public final static String GRANT_NUMBER_AGENCY = "grantNumberAgency";
    public final static String GRANT_NUMBER_VALUE = "grantNumberValue";
    public final static String DISTRIBUTOR = "distributor";
    public final static String DISTRIBUTOR_NAME = "distributorName";
    public final static String DISTRIBUTOR_AFFILIATION = "distributorAffiliation";
    public final static String DISTRIBUTOR_ABBREVIATION = "distributorAbbreviation";
    public final static String DISTRIBUTOR_URL = "distributorURL";
    public final static String DISTRIBUTOR_LOGO_URL = "distributorLogoURL";
    public final static String DISTRIBUTION_DATE = "distributionDate";
    public final static String DEPOSITOR = "depositor";
    public final static String DATE_OF_DEPOSIT = "dateOfDeposit";
    public final static String TIME_PERIOD_COVERED = "timePeriodCovered";
    public final static String TIME_PERIOD_COVERED_START = "timePeriodCoveredStart";
    public final static String TIME_PERIOD_COVERED_END = "timePeriodCoveredEnd";
    public final static String DATE_OF_COLLECTION = "dateOfCollection";
    public final static String DATE_OF_COLLECTION_START = "dateOfCollectionStart";
    public final static String DATE_OF_COLLECTION_END = "dateOfCollectionEnd";
    public final static String KIND_OF_DATA = "kindOfData";
    public final static String SERIES = "series";
    public final static String SERIES_NAME = "seriesName";
    public final static String SERIES_INFORMATION = "seriesInformation";
    public final static String SOFTWARE = "software";
    public final static String SOFTWARE_NAME = "softwareName";
    public final static String SOFTWARE_VERSION = "softwareVersion";
    public final static String RELATED_MATERIAL = "relatedMaterial";
    public final static String RELATED_DATASETS = "relatedDatasets";
    public final static String OTHER_REFERENCES = "otherReferences";
    public final static String DATA_SOURCES = "dataSources";
    public final static String ORIGIN_OF_SOURCES = "originOfSources";
    public final static String CHARACTERISTICS_OF_SOURCES = "characteristicOfSources";
    public final static String ACCESS_TO_SOURCES = "accessToSources";

    public final static String RIGHTS_HOLDER = "dansRightsHolder";
    public final static String PERSONAL_DATA_PRESENT = "dansPersonalDataPresent";
    public final static String LANGUAGE_OF_METADATA = "dansMetadataLanguage";
    public final static String AUDIENCE = "dansAudience";

    public final static String COLLECTION = "dansCollection";

    public final static String RELATION = "dansRelation";
    public final static String RELATION_TYPE = "dansRelationType";
    public final static String RELATION_URI = "dansRelationURI";
    public final static String RELATION_TEXT = "dansRelationText";

    public final static String ARCHIS_ZAAK_ID = "dansArchisZaakId";
    public final static String ARCHIS_NUMBER = "dansArchisNumber";
    public final static String ARCHIS_NUMBER_TYPE = "dansArchisNumberType";
    public final static String ARCHIS_NUMBER_ID = "dansArchisNumberId";
    public final static String ABR_RAPPORT_TYPE = "dansAbrRapportType";
    public final static String ABR_RAPPORT_NUMMER = "dansAbrRapportNummer";
    public final static String ABR_VERWERVINGSWIJZE = "dansAbrVerwervingswijze";
    public final static String ABR_COMPLEX = "dansAbrComplex";
    public final static String ABR_ARTIFACT = "dansAbrArtifact";
    public final static String ABR_PERIOD = "dansAbrPeriod";

    public final static String ABR_BASE_URL = "https://data.cultureelerfgoed.nl/term/id/abr";
    public final static String SCHEME_ABR_OLD = "Archeologisch Basis Register";
    public final static String SCHEME_URI_ABR_OLD = "https://data.cultureelerfgoed.nl/term/id/rn/a4a7933c-e096-4bcf-a921-4f70a78749fe";
    public final static String SCHEME_ABR_PLUS = "Archeologisch Basis Register";
    public final static String SCHEME_URI_ABR_PLUS = "https://data.cultureelerfgoed.nl/term/id/abr/b6df7840-67bf-48bd-aa56-7ee39435d2ed";

    public final static String SCHEME_ABR_COMPLEX = "ABR Complextypen";
    public final static String SCHEME_URI_ABR_COMPLEX = "https://data.cultureelerfgoed.nl/term/id/abr/e9546020-4b28-4819-b0c2-29e7c864c5c0";

    public final static String SCHEME_ABR_ARTIFACT = "ABR Artefacten";
    public final static String SCHEME_URI_ABR_ARTIFACT = "https://data.cultureelerfgoed.nl/term/id/abr/22cbb070-6542-48f0-8afe-7d98d398cc0b";

    public final static String SCHEME_ABR_PERIOD = "ABR Periodes";
    public final static String SCHEME_URI_ABR_PERIOD = "https://data.cultureelerfgoed.nl/term/id/abr/9b688754-1315-484b-9c89-8817e87c1e84";

    public final static String SCHEME_ABR_RAPPORT_TYPE = "ABR Rapporten";
    public final static String SCHEME_URI_ABR_RAPPORT_TYPE = "https://data.cultureelerfgoed.nl/term/id/abr/7a99aaba-c1e7-49a4-9dd8-d295dbcc870e";

    public final static String SCHEME_ABR_VERWERVINGSWIJZE = "ABR verwervingswijzen";
    public final static String SCHEME_URI_ABR_VERWERVINGSWIJZE = "https://data.cultureelerfgoed.nl/term/id/abr/554ca1ec-3ed8-42d3-ae4b-47bcb848b238";

    public final static String TEMPORAL_COVERAGE = "dansTemporalCoverage";
    public final static String SPATIAL_POINT = "dansSpatialPoint";
    public final static String SPATIAL_POINT_SCHEME = "dansSpatialPointScheme";
    public final static String SPATIAL_POINT_X = "dansSpatialPointX";
    public final static String SPATIAL_POINT_Y = "dansSpatialPointY";
    public final static String SPATIAL_BOX = "dansSpatialBox";
    public final static String SPATIAL_BOX_SCHEME = "dansSpatialBoxScheme";
    public final static String SPATIAL_BOX_NORTH = "dansSpatialBoxNorth";
    public final static String SPATIAL_BOX_EAST = "dansSpatialBoxEast";
    public final static String SPATIAL_BOX_SOUTH = "dansSpatialBoxSouth";
    public final static String SPATIAL_BOX_WEST = "dansSpatialBoxWest";
    public final static String SPATIAL_COVERAGE_CONTROLLED = "dansSpatialCoverageControlled";
    public final static String SPATIAL_COVERAGE_UNCONTROLLED = "dansSpatialCoverageText";
    public static final String DATAVERSE_PID = "dansDataversePid";
    public static final String DATAVERSE_PID_VERSION = "dansDataversePidVersion";
    public static final String BAG_ID = "dansBagId";
    public static final String NBN = "dansNbn";
    public static final String DANS_OTHER_ID = "dansOtherId";
    public static final String DANS_OTHER_ID_VERSION = "dansOtherIdVersion";
    public static final String SWORD_TOKEN = "dansSwordToken";
    public static final String DATA_SUPPLIER = "dansDataSupplier";
}
