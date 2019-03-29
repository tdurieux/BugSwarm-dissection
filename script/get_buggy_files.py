import os
import json
import requests

ROOT = os.path.join(os.path.dirname(__file__), '..')

data = None
with open(os.path.join(ROOT, 'bugswarm.json')) as fd:
    data = json.load(fd)

travis_jobs = {}
travis_jobs_path = os.path.join(ROOT, 'travis_data.json')
if os.path.exists(travis_jobs_path):
    with open(travis_jobs_path) as fd:
        travis_jobs = json.load(fd)


def get_changed_files(diff):
    files = []
    diff_splitted = diff.split('\n')
    
    for i in range(0, len(diff_splitted)):
        line = diff_splitted[i]
        if '---' == line[:3] and line[4:] != '/dev/null':
            if '+++' == diff_splitted[i+1][:3]:
                files.append(line[6:])
    return list(set(files))

count = 0
for bug in data:
    failed_job = travis_jobs[str(bug['failed_job']['job_id'])]
    repo = failed_job['repository_slug']
    commit_id = failed_job['commit']['sha']

    count += 1

    bug_id = "%s-%s" % (bug['repo'].replace('/', '-'), failed_job['id'])

    diff_path = os.path.join(ROOT, 'diffs', bug_id, 'patch.diff')
    diff = None
    with open(diff_path) as fd:
        diff = fd.read()
    output_diff = os.path.join(ROOT, 'BugSwarm', bug_id, 'patch.diff')
    if not os.path.exists(os.path.dirname(output_diff)):
        os.makedirs(os.path.dirname(output_diff))
    if not os.path.exists(output_diff):
        with open(output_diff, 'w') as fd:
            fd.write(diff)
    files = get_changed_files(diff)
    print('%s/%s' %(count, len(data)))
    root_buggy = os.path.join(ROOT, 'BugSwarm', bug_id, 'buggy_files/')
    for (root,d,fs)  in os.walk(root_buggy):
        for f in fs:
            if os.path.join(root, f).replace(root_buggy, '') not in files:
                os.remove(os.path.join(root, f))
                print(os.path.join(root, f).replace(root_buggy, ''))
    for f in files:
        if f == '':
            continue
        output_path = os.path.join(ROOT, 'BugSwarm', bug_id, 'buggy_files', f)
        if os.path.exists(output_path):
            continue
        content = requests.get("https://raw.githubusercontent.com/%s/%s/%s" % (repo, commit_id, f)).content
        
        if not os.path.exists(os.path.dirname(output_path)):
            os.makedirs(os.path.dirname(output_path))
        with open(output_path,'wb') as fd:
            try:
                fd.write(content)
            except Exception as identifier:
                print(identifier)
    
