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

def reshape(lst, n):
    return [lst[i*n:(i+1)*n] for i in range(len(lst)//n)]

job_ids = []
for bug in data:
    if str(bug['failed_job']['job_id']) not in travis_jobs:
        job_ids.append(str(bug['failed_job']['job_id']))
    if str(bug['passed_job']['job_id']) not in travis_jobs:
        job_ids.append(str(bug['passed_job']['job_id']))

    # print(count, bug['repo'], bug['failed_job']['job_id'], bug['passed_job']['job_id'], bug['stability'])

groups = reshape(job_ids, 150)
print(len(groups))


for group in groups:
    ids = '?ids[]=' + '&ids[]='.join(group)
    r = requests.get("https://api.travis-ci.org/jobs%s" % ids, headers={'Accept': 'application/vnd.travis-ci.2+json, */*; q=0.01'})
    results = r.json()
    index = 0
    for job in results['jobs']:
        travis_jobs[job['id']] = job
        travis_jobs[job['id']]['commit'] = results['commits'][index]
        index += 1

with open(travis_jobs_path, 'w') as fd:
    json.dump(travis_jobs, fd) 

for bug in data:
    failed_job = travis_jobs[str(bug['failed_job']['job_id'])]
    passed_job =  travis_jobs[str(bug['passed_job']['job_id'])]

    bug_id = "%s-%s" % (bug['repo'].replace('/', '-'), failed_job['id'])

    bug_path = os.path.join(ROOT, 'diffs', bug_id)

    if os.path.exists(os.path.join(bug_path, 'patch.diff')):
        continue

    url = "https://github.com/%s/compare/%s...%s.diff" % (bug['repo'], failed_job['commit']['sha'], passed_job['commit']['sha'])
    r = requests.get(url)
    content = r.text

    if not os.path.exists(bug_path):
        os.makedirs(bug_path)
    
    with open(os.path.join(bug_path, 'patch.diff'), 'w') as fd:
        fd.write(content)
    
