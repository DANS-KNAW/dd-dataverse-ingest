DESCRIPTION
===========

Service for ingesting datasets into Dataverse via the API.

Deposit directories
-------------------

The datasets are prepared as deposit directories in the ingest area. There are the following types of deposit directories:

### `simple`

A directory with the following structure:

```text
<uuid>/
  ├── deposit.properties
  ├── dataset.yml
  ├── files/
  │   ├── file1.txt
  │   ├── file2.txt
  │   └── subdirectory/
  │       └── file3.txt
```

The name of the deposit directory must be a UUID. The deposit directory contains the following files:

| File                 | Description                                                                                                 |
|----------------------|-------------------------------------------------------------------------------------------------------------|
| `deposit.properties` | Contains instructions for `dd-dataverse-ingest` on how to ingest the dataset.                               |
| `dataset.yml`        | Contains metadata for the dataset in Yaml compatible with the Native API format that Dataverse<br> expects. |
| `files/`             | Contains the files that are part of the dataset; subdirectories are translated<br>into directoryLabels.     |

### `dans-bag`: TODO

Processing
----------
The deposit area is a directory with the following structure:

```text
imports
├── inbox
│   └── path
│       └── to
│           ├── batch1
│           │   ├── 0223914e-c053-4ee8-99d8-a9135fa4db4a
│           │   ├── 1b5c1b24-de40-4a40-9c58-d4409672229e
│           │   └── 9a47c5be-58c0-4295-8409-8156bd9ed9e1
│           └── batch2
│               ├── 5e42a936-4b90-4cac-b3c1-798b0b5eeb0b
│               └── 9c2ce5a5-b836-468a-89d4-880efb071d9d
└── outbox
    └── path
        └── to
            └── batch1
                ├── failed
                ├── processed
                │   └── 7660539b-6ddb-4719-aa31-a3d1c978081b
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

