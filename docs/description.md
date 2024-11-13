DESCRIPTION
===========

Service for ingesting datasets into Dataverse via the API.

Deposit directories
-------------------

The datasets are prepared as deposit directories (or "deposits" for short) in the ingest area. There are the following types of deposit directories:

### Simple

A directory with the following structure:

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

In the root of the bag, the following files can be included to provide metadata and instructions for the ingest process

* `dataset.yml`: Dataset level metadata for the dataset in YAML format. It corresponds the JSON that is passed to the `createDataset` and `updateMetadata` API
  calls.
* `files.yml`: File level metadata for the files in the dataset in YAML format. It corresponds to the JSON that is passed to the `addFile` API call.
* `edit.yml`: Instructions to delete or replace specific files. An example of the content of this file is:

<!-- TODO: elaborate this -->

```yaml
fileActions:
  - action: DELETE
    filename: file1.txt
  - action: REPLACE
    filename: file2.txt
    replacement: /path/to/new/file2.txt
```

#### New versions of existing datasets

<!-- TODO: simplify this ? -->

A deposit can also be used to create a new version of an existing dataset. The `bag-info.txt` file must contain the tag `Updates-Dataset`: <doi>` where `<doi>
` is the DOI of the dataset to be updated with the prefix `doi:`, as Dataverse requires.

Instead of one bag directory, the deposit may contain multiple bags. In this case the names of the bag must contain a numeric prefix, followed by a dash.
Alternatively, the name of the bag may be only a number. The numbers may have leading zeros. The numbers determine the order of processing, but are not included
in the Dataverse metadata.

If the first bag represents a new dataset it MUST use number 0. If the first bag represents a new version of an existing dataset it MUST use a number greater
than 0.

Examples:

```text
# New dataset with two versions
<uuid>
├── 0-bag
..
└── 1-bag 

# One bag, which updates an existing dataset
<uuid>
└── bag
     └── bag-info.txt
            Updates-Dataset: doi:10.5072/FK2/ABCDEF

# Two bags, which update an existing dataset (adding two new versions)
<uuid>
├── 1-bag
     └── bag-info.txt
            Updates-Dataset: doi:10.5072/FK2/ABCDEF
..
└── 2-bag
```

[BagIt]: {{ bagit_specs_url }}

### DANS bag

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

The deposits to be processed are to be placed under `inbox`. All the files in it must be readable and writable by the service.
When the service is requested to process a batch, it will do the folowing for each deposit:

1. Create a dataset in Dataverse using the metadata in `dataset.yml`.
2. Upload the files in `files/` to the dataset.
3. Publish the dataset.
4. Wait for the dataset to be published.
5. Move the deposit to `outbox/path/to/batch/processed` if the dataset was published successfully, to
   `outbox/path/to/batch/rejected` if the dataset was not valid, or to `outbox/path/to/batch/failed` if some
   other error occurred.

Note that the relative path of the processed files in outbox is the same as in the inbox, except for an extra level
of directories for the status of the deposit.

