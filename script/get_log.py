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

count = 0
for bug in data:
    failed_job = travis_jobs[str(bug['failed_job']['job_id'])]
    repo = failed_job['repository_slug']
    commit_id = failed_job['commit']['sha']

    count += 1

    bug_id = "%s-%s" % (bug['repo'].replace('/', '-'), failed_job['id'])

    output_path = os.path.join(ROOT, 'BugSwarm', bug_id, 'travis.log')
    if os.path.exists(output_path):
        continue
    
    print('%s/%s' %(count, len(data)))

    r = requests.get("https://api.travis-ci.org/jobs/%s/log" % failed_job['id'], headers={'Accept': 'application/vnd.travis-ci.2+json, */*; q=0.01'})

    with open(output_path,'wb') as fd:
        try:
            fd.write(r.content)
        except Exception as identifier:
            print(identifier)