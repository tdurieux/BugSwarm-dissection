import os
import json
import requests
import hashlib

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

global_file_types = {}

count = 0
count_no_source_change = {
    'Java': 0,
    'Python': 0
}
count_test_failings = {
    'Java': 0,
    'Python': 0
}
md5_diff = {}
count_duplicate_diff = 0
count_only_test_change = {
    'Java': 0,
    'Python': 0
}
count_only_source_change = {
    'Java': 0,
    'Python': 0
}
count_test_change = 0
empty = 0

final_bench = []

for bug in data:
    failed_job = travis_jobs[str(bug['failed_job']['job_id'])]
    repo = failed_job['repository_slug']
    commit_id = failed_job['commit']['sha']

    count += 1

    bug_id = "%s-%s" % (bug['repo'].replace('/', '-'), failed_job['id'])

    diff_path = os.path.join(ROOT, 'diffs', bug_id, 'patch.diff')
    diff = None
    with open(diff_path,encoding='utf-8') as fd:
        diff = fd.read()
    
    m = hashlib.md5()
    m.update(diff.encode('utf-8'))
    key = m.hexdigest()

    is_duplicate = False
    if key in md5_diff:
        if len(diff) > 0:
            count_duplicate_diff += 1
            is_duplicate = True
    else:
        md5_diff[key] = 0
    md5_diff[key] += 1

    files = get_changed_files(diff)

    print('%s/%s' %(count, len(data)))
    root_buggy = os.path.join(ROOT, 'BugSwarm', bug_id, 'buggy_files/')

    file_types = {}
    tests = []
    for f  in files:
        if 'tests/' in f or 'test/' in f or 'Test.java' in f or 'test_' in f or 'spec/' in f or 'checks.py' in f:
            count_test_change += 1
            tests.append(f)
        extension = os.path.basename(f).lower()
        try:
            index = extension.rindex('.')
            extension = extension[index+1:]
        except Exception:
            pass
        if extension not in global_file_types:
            global_file_types[extension] = 0    
        global_file_types[extension] += 1

        if extension not in file_types:
            file_types[extension] = 0    
        file_types[extension] += 1
    is_only_test = len(tests) == len(files) and len(files) > 0
    if len(files) == 0:
        empty += 1        
    elif bug['lang'] == 'Java':
        if is_only_test:
            count_only_test_change['Java'] += 1
        if len(bug['failed_job']['failed_tests']) > 0 and not is_duplicate and not is_only_test:
            if 'java' in file_types:
                count_test_failings['Java'] += 1
                final_bench.append(bug)
                if len(tests) == 0:
                    count_only_source_change['Java'] += 1
        if 'java' not in file_types:
            count_no_source_change['Java'] += 1            
    elif bug['lang'] == 'Python':
        if is_only_test:
            count_only_test_change['Python'] += 1
        if len(bug['failed_job']['failed_tests']) > 0 and not is_duplicate and not is_only_test:
            if 'py' in file_types:
                count_test_failings['Python'] += 1
                final_bench.append(bug)
                if len(tests) == 0:
                    count_only_source_change['Python'] += 1
        if 'py' not in file_types:
            count_no_source_change['Python'] += 1
print(md5_diff)
print(global_file_types)
print('count_no_source_change', count_no_source_change)
print('empty', empty)
print('count_test_change', count_test_change)
print('count_duplicate_diff', count_duplicate_diff)
print('count_test_failings', count_test_failings)
print('count_only_test_change', count_only_test_change)
print('count_only_source_change', count_only_source_change)
with open(os.path.join(ROOT, 'final_bugswarm.json'), 'w') as fd:
    json.dump(final_bench, fd)