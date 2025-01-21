DESCRIPTION
===========

Service for ingesting datasets into Dataverse via the API.

Deposit directories
-------------------

The datasets are prepared as deposit directories (or "deposits" for short) in the ingest area. A deposit is a directory with the following structure:

```text
087920d1-e37d-4263-84c7-1321e3ecb5f8
├── bag
│   ├── bag-info.txt
│   ├── bagit.txt
│   ├── data
│   │   ├── file1.txt
│   │   ├── file2.txt
│   │   └── subdirectory
│   │       └── file3.txt
│   ├── dataset.yml
│   └── manifest-sha1.txt
└── deposit.properties
```

The name of the deposit directory must be a UUID. The deposit directory contains the following files:

| File                 | Description                                                                                                                                                                                                  |
|----------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `deposit.properties` | Contains instructions for `dd-dataverse-ingest` on how to ingest the dataset.                                                                                                                                |
| `bag/`               | A bag, i.e. a directory with the files to be ingested, laid out according to <br>the [BagIt]{:target=_blank} specification. <br>The name of the bag does not have to be "bag"; it may be any valid filename. |

Instead of one bag multiple bags may be included, see [below](#new-versions-of-existing-datasets).

#### Metadata and instructions

In the root of the bag, the following files can be included to provide metadata and instructions for the ingest process. The files are in YAML format and
closely follow the JSON that is passed to the Dataverse API.

| File                   | Description                                                                                                                              |
|------------------------|------------------------------------------------------------------------------------------------------------------------------------------|
| `init.yml`             | Preconditions and instructions for creating a new dataset.                                                                               |
| `dataset.yml`          | Dataset level metadata.                                                                                                                  |
| `edit-files.yml`       | Instructions for deleting, replacing or moving files, or updating the file metadata;<br> also included: restricting and embargoing files |
| `edit-metadata.yml`    | Edit dataset level metadata, including metadata value deletions                                                                          |
| `edit-permissions.yml` | Role assignments to create or delete on the dataset                                                                                      |
| `update-state.yml`     | Whether to publish the dataset version or submit it for review                                                                           |

##### init.yml

The init file initializes the ingest process. It can be used to verify that an expected precondition is met:

```yaml
init:
  expect:
    state: 'released' # or 'draft'
    dataverseRoleAssignment:
      assignee: '@myuser'
      role: ':autenticated-user'
    datasetRoleAssignment:
      assignee: '@myuser'
      role: 'contributor'

```

If the state of the dataset does not match the expected state, the ingest procedure will be aborted and the deposit will be put in the FAILED state. The
expected state can be either `released` or `draft`. By default, no check will be performed.

If the role assignment in  `dataverseRoleAssignment` does not exist on the target dataverse collection (currently always `root`) the ingest procedure will be
aborted putting the deposit in a REJECTED state. The same happens if the role assignment in `datasetRoleAssignment` does not exist on the target dataset
(only applicable to update-deposits).

Note the difference in the resulting deposit state when expectations are not met. The rationale is that an unexpected dataset state is likely an error by the
service or Dataverse and the expected role assignments are a kind of authorization check.

The `init.yml` file can also be used to instruct the service to import the bag as a dataset with an existing DOI:

```yaml
init:
  create:
    importPid: 'doi:10.5072/FK2/ABCDEF'
```

In this case the `updates-dataset` property in `deposit.properties` should not be set. It will be ignored if it is. By default, a new dataset will be created,
whose persistent identifier will be assigned by Dataverse.

The user is responsible for providing expectations and instructions that do not conflict with each other. For example, if the `importPid` property is set, and
the `state` property is set to `released`, the service will either abort because the dataset already exists, or it will fail to import the dataset, because
the dataset already exists.

##### dataset.yml

The format is the same as the JSON that is passed to the [createDataset]{:target=_blank} endpoint of the Dataverse API. Note that the `files` field is not used.
It will be set to the empty list by the service, because otherwise Dataverse will reject the request.

```yaml
datasetVersion:
  license:
    name: "CC0 1.0"
    uri: "http://creativecommons.org/publicdomain/zero/1.0"
  fileAccessRequest: true
  metadataBlocks:
    citation:
      displayName: "Citation Metadata"
      name: "citation"
      fields:
        - typeName: "title"
          multiple: false
          typeClass: "primitive"
          value: "My dataset"
      # Add more metadata  fields and blocks as needed
```

[createDataset]: {{ dataverse_api_url }}/native-api.html#create-a-dataset-in-a-dataverse-collection

##### edit-files.yml

See the inline comments for more information. The steps are performed in the order here given. You **cannot** change the order of the steps by changing the
order
of the items in the file.

The actions specified in this file correspond roughly to the actions available in the dropdown menu in the file view of a dataset in Dataverse.

`updateFileMetas` contains items in the format of the JSON that is passed to the [updateFileMetadata]{:target=_blank} endpoint of the Dataverse API.

[updateFileMetadata]: {{ dataverse_api_url }}/native-api.html#updating-file-metadata

```yaml
editFiles:
  # Deletes the files from the dataset. The files are looked up by their path in the dataset. Note, that it is possible to add files back to the same location 
  # in the dataset in the same deposit. In that case, there will be no continuous history of the file in the dataset.  
  deleteFiles:
    - 'file1.txt'
    - 'subdirectory/file3.txt'
  # Replaces the files in the dataset. The files are looked up by their path in the dataset. The replacement file is looked up in the bag, under the `data`
  # directory under the same path as the original file has in the dataset. Note that files in `replaceFiles` will automatically be skipped in the add files step, 
  replaceFiles:
    - 'file2.txt'
  # Adds files to the dataset and makes them unrestricted. The files are processed in batches, meaning they are uploaded as ZIP files to Dataverse, for Dataverse 
  # to unzip them. This is more efficient than adding the files one by one.
  addUnrestrictedFiles:
     - 'file6.txt'
  # Adds files to the dataset and makes them restricted. The files are processed in batches, meaning they are uploaded as ZIP files to Dataverse, for Dataverse
  # to unzip them. This is more efficient than adding the files one by one. 
  addRestrictedFiles:
    - 'file4.txt'
    - 'subdirectory/file5.txt'
  # Adds files to the dataset and makes them unrestricted. The files are processed one by one, meaning they are uploaded as individual files to Dataverse. This is
  # useful if you need to circumvent special processing by Dataverse, such as re-zipping Shapefile projects. 
  # See: https://guides.dataverse.org/en/6.3/developers/geospatial.html#geospatial-data 
  # 
  # Conversely, you could use this to make sure a ZIP file in your bag is expanded by Dataverse, since Dataverse will not expand ZIP files that are uploaded 
  # inside another ZIP file, as is the case with the batch upload method.
  addUnrestrictedFilesIndividually:
     - 'bicycles.shp'
     - 'bicycles.shx'
     - 'bicycles.dbf'
     - 'bicycles.prj'
  # The same as above, but the files are added restricted.
  addRestrictedFilesIndividually:
    - 'bicycles.shp'
    - 'bicycles.shx'
    - 'bicycles.dbf'
    - 'bicycles.prj'
  # Moves files in the dataset. This is essentially a metadata change: the label and/or directoryLabel of the file is changed.
  moveFiles:
    - from: 'file6.txt' # Old location in the dataset
      to: 'subdirectory/file6.txt' # New location in the dataset
  # Updates the metadata of the files in the dataset. The files are looked up by their path in the dataset, so it is not possible to change the label or 
  # directoryLabel of the file in this step; use moveFiles for that.
  updateFileMetas:
    - description: "This is the first file"
      label: "file1.txt"
      directoryLabel: "subdirectory"
      restricted: false
      categories: [ 'Testlabel' ]
  # Sets one or more embargoes on the files in the dataset.     
  addEmbargoes:
    - filePaths: [ 'file1.txt' ] # All other files will NOT be embargoed
      dateAvailable: '2030-01-01'
      reason: 'Pending publication'
   # This is not a separate step, but the auto-renaming takes place whenever a local filepath is translated to a dataset filepath.    
  autoRenameFiles:
     - from: "Unsanitize'd/file?" # Local file name
       to: "Sanitize_d/file_" # The file name assigned in the dataset
```

##### edit-metadata.yml

```yaml
editMetadata:
  addFieldValues:
    - typeName: "subject"
      typeClass: "controlledVocabulary"
      multiple: true
      value:
        - 'Astronomy and Astrophysics'
  replaceFieldValues:
    - typeName: "producer"
      typeClass: "compound"
      multiple: true
      value:
        - producerName:
            typeName: "producerName"
            value: "John Doe"
        - producerAffiliation:
            typeName: "producerAffiliation"
            value: "University of Somewhere"
  deleteFieldValues:
    - typeName: "subject"
      typeClass: "controlledVocabulary"
      multiple: true
      value:
        - 'Astronomy and Astrophysics'
```

Allows you to selectively delete, add or replace metadata field values. The format is the based on the JSON that is passed to the
[editDatasetMetadata]{:target=_blank} and [deleteDatasetMetadata]{:target=_blank} endpoints of the Dataverse API. However, unlike in the JSON accepted by
Dataverse, the `typeClass` and `multiple fields are ___**not optional**___ in the YAML file. This is due to the library used to parse the YAML files, which uses
a deserializer that was designed to parse the JSON that is returned by the Dataverse API (which does not include these fields).

The only difference between `addFieldValues` and `replaceFieldValues` is that the latter will pass the `replace=true` parameter to the API. See the API
documentation for the exact behavior.

Unlike in the editing of files, deletion of field values takes place at the end of the process, so that we don't create a situation where a required field is
temporarily empty and Dataverse refuses to save the metadata.

[editDatasetMetadata]: {{ dataverse_api_url }}/native-api.html#edit-dataset-metadata
[deleteDatasetMetadata]: {{ dataverse_api_url }}/native-api.html#delete-dataset-metadata

##### edit-permissions.yml

```yaml
editPermissions:
  deleteRoleAssignments:
    - role: 'admin'
      assignee: '@user1'
  addRoleAssignments:
    - role: 'admin'
      assignee: '@user2'      
```

Allows you to selectively delete or add role assignments on the dataset. The format is the same as the JSON that is passed to the
[assignNewRole]{:target=_blank} and [deleteRoleAssignments]{:target=_blank} endpoints of the Dataverse API.

[assignNewRole]: {{ dataverse_api_url }}/native-api.html#assign-a-new-role-on-a-dataset
[deleteRoleAssignments]: {{ dataverse_api_url }}/native-api.html#delete-role-assignment-on-a-dataset

##### update-state.yml

```yaml
updateState:
  publish: major # or 'minor'
```

```yaml
updateState:
  releaseMigrated: 2021-01-01
```

#### New versions of existing datasets

A deposit can also be used to create a new version of an existing dataset. In this case, the `deposit.properties` file must contain the following property:

```text
updates-dataset: 'doi:10.5072/FK2/ABCDEF'
```

in which the value is the DOI of the dataset to be updated.

Instead of one bag directory, the deposit may contain multiple bags. In this case the directories are processed in lexicographical order, so you should name the
bags accordingly, e.g. `1-bag`, `2-bag`, `3-bag`, etc. , or `001-bag`, `002-bag`, `003-bag`, etc., depending on the number of bags.

[BagIt]: {{ bagit_specs_url }}

### DANS bag

A DANS bag is a directory in the [BagIt]{:target=_blank} format, that also conforms to the [DANS bag profile]{:target=_blank}. This is a legacy format that is
used by the DANS SWORD2 service. The service can convert a DANS deposit to the standard one described above.

<!-- TODO: elaborate -->

[BagIt]: {{ bagit_specs_url }}
[DANS bag profile]: {{ dans_bag_profile_url }}

Processing
----------
The deposit area is a directory with the following structure:

```text
imports
├── inbox
│   └── path
│       └── to
│           ├── batch1
│           │   ├── 0223914e-c053-4ee8-99d8-a9135fa4db4a
│           │   ├── 1b5c1b24-de40-4a40-9c58-d4409672229e
│           │   └── 9a47c5be-58c0-4295-8409-8156bd9ed9e1
│           └── batch2
│               ├── 5e42a936-4b90-4cac-b3c1-798b0b5eeb0b
│               └── 9c2ce5a5-b836-468a-89d4-880efb071d9d
└── outbox
    └── path
        └── to
            └── batch1
                ├── failed
                ├── processed
                │   └── 7660539b-6ddb-4719-aa31-a3d1c978081b
                └── rejected
```

### Processing a batch

The deposits to be processed are to be placed under `inbox`. All the files in it must be readable and writable by the service.
When the service is requested to process a batch, it will do the following:

1. Sort the deposits in the batch by their `creation.timestamp` property in `deposit.properties`, in ascending order.
2. Process each deposit in the batch in order.

### Processing a deposit

1. Sort the bags in the deposit by lexicographical order.
2. Process each bag in the deposit in order.
3. Move the deposit to:
    * `outbox/path/to/batch/processed` if the all versions were published successfully, or to
    * `outbox/path/to/batch/rejected` if one or more of the versions were not valid, or to
    * `outbox/path/to/batch/failed` if some other error occurred.

Note that the relative path of the processed deposits in outbox is the same as in the inbox, except for an extra level of directories for the status of the
deposit.

### Processing a bag

The actions described in the Yaml files will be executed in same order as they are listed above. Note that changing the order of the actions in the Yaml files
has no effect on the order in which they are executed. All files and all action fields (e.g., `addRestrictedFiles`) are optional, except for `dataset.yml`, when
creating a new dataset.

### The task log

The service keeps the progress of the processing in file called `_tasks.yml`. Its layout corresponds closely to the combined layout of the instruction Yaml
files. (The underscore in the name is there to make it stand out in the directory listing.):
    
```yaml
taskLog:
   init:
      targetPid: null
      expect:
         state:
            completed: false
         dataverseRoleAssignment:
            completed: false
         datasetRoleAssignment:
            completed: false
      create:
         completed: false
   dataset:
      completed: false
   editFiles:
      deleteFiles:
         completed: false
         numberCompleted: 0
      replaceFiles:
         completed: false
         numberCompleted: 0
      addUnrestrictedFiles:
         completed: false
         numberCompleted: 0
      addRestrictedFiles:
         completed: false
         numberCompleted: 0
      addUnrestrictedIndividually:
         completed: false
         numberCompleted: 0
      addRestrictedIndividually:
         completed: false
         numberCompleted: 0
      moveFiles:
         completed: false
         numberCompleted: 0
      updateFileMetas:
         completed: false
         numberCompleted: 0
      addEmbargoes:
         completed: false
         numberCompleted: 0
      editMetadata:
         addFieldValues:
            completed: false
         replaceFieldValues:
            completed: false
         deleteFieldValues:
            completed: false
      editPermissions:
         deleteRoleAssignments:
            completed: false
            numberCompleted: 0
         addRoleAssignments:
            completed: false
            numberCompleted: 0
      updateState:
         completed: false
```

The file is updated in memory and will be written to the root of the bag when the processing of the bag is finished or fails. If at the start of processing the
bag the file is found in the root of the bag, the service will continue from where it left off. Note, that some items have a `numberCompleted` field, so if the
overall task is not yet completed, the service will continue from where it left off.

If you want to re-ingest a deposit completely, delete the `_tasks.yml` file from the root of the deposit. You should probably also delete the dataset version 
that was created in the previous run.
