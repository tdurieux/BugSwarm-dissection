import os
import json
import requests

ROOT = os.path.join(os.path.dirname(__file__), '..')

data = None
with open(os.path.join(ROOT, 'final_bugswarm.json')) as fd:
    data = json.load(fd)

travis_jobs = {}
travis_jobs_path = os.path.join(ROOT, 'travis_data.json')
if os.path.exists(travis_jobs_path):
    with open(travis_jobs_path) as fd:
        travis_jobs = json.load(fd)

output = []

for bug in data:
    failed_job = travis_jobs[str(bug['failed_job']['job_id'])]
    passed_job =  travis_jobs[str(bug['passed_job']['job_id'])]

    bug_id = "%s-%s" % (bug['repo'].replace('/', '-'), failed_job['id'])

    diff_path = os.path.join(ROOT, 'diffs', bug_id, 'patch.diff')

    with open(diff_path) as fd:
        bug['diff'] = fd.read()
    
    added_lines = 0
    removed_lines = 0
    patch_size = 0
    nb_files = 0

    diff_splitted = bug['diff'].split('\n')
    
    for line in diff_splitted:
        first = ''
        if len(line) > 0:
            first = line[0]
        if '---' == line[:3]:
            nb_files += 1
        elif '+++' == line[:3]:
            continue
        elif first == '+':
            added_lines += 1
            patch_size += 1
        elif first == '-':
            removed_lines += 1
            patch_size += 1
    if (added_lines == 1 and (removed_lines == 1 or removed_lines == 0)) or (removed_lines == 1 and (added_lines == 1 or added_lines == 0)):
        bug['repairPatterns'] = {
            'singleLine': 1
        }
    bug['metrics'] = {
        'addedLines': added_lines,
        'removedLines': removed_lines,
        'patchSize': patch_size,
        'nbFiles': nb_files
    }
    bug['benchmark'] = 'BugSwarm'
    bug['bugId'] = bug_id
    bug['travis'] = {
        'failed_job': failed_job,
        'passed_job': passed_job
    }
    output.append(bug)

with open(os.path.join(ROOT, 'docs', 'data', 'bugswarm.json'), 'w') as fd:
    json.dump(output, fd)