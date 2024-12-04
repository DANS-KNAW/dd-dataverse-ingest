revisions
=========

The bags revision01, revision02 and revision03 demonstrate how deposits can update existing datasets: revision01 creates a new
dataset and revision02 and revision03 create versions 2 and 3.

Revision01
----------
Revision01 has a file (fileD) with accessibleToRights set to RESTRICTED_REQUEST. After revision01 is ingested, the
dataset has File Access Requests enabled. No Terms of Access are filled in. They could be filled in but are
not mandatory, as they can be negotiated via a request.

Revision02
----------
In revision 02 fileD has been further restricted to accessibleToRights NONE. This means that access can no longer be
requested. Since File Access Requests is a dataset-level attribute, this mean that the dataset has been closed for
access requests for all its restricted files in all versions including future versions. Opening it again cannot be
done via automated deposit, and requires manual actions by the administrator.

An alternative way to close the dataset for file access requests is to set ddm:accessRights in dataset.xml to NO_ACCESS. 

In revision02 the available date is set to 2025. If, by the time you are reading this, this is still in the future, this would
embargo any files added to the new version, either as new files or as replacements for existing files.

Revision03
----------
In revision 03 fileD has been reset to RESTRICTED_REQUEST. As explained in the previous section, this does 
NOT enable file access request for the dataset again, because this is a dataset-level attribute.
