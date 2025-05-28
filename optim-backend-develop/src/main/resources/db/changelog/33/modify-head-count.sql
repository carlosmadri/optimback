ALTER TABLE head_count DROP COLUMN description;
ALTER TABLE head_count DROP COLUMN exercise;
ALTER TABLE head_count ADD COLUMN ref_count VARCHAR(255);