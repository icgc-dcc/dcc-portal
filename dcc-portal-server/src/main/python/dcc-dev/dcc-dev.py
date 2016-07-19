    #!/usr/bin/python
#
# Copyright (c) 2016 The Ontario Institute for Cancer Research. All rights reserved.
#
# This program and the accompanying materials are made available under the terms of the GNU Public License v3.0.
# You should have received a copy of the GNU General Public License along with
# this program. If not, see <http://www.gnu.org/licenses/>.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
# EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
# OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT
# SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
# INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
# TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
# OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
# IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
# ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

from flask import Flask, render_template, request, send_from_directory
import json
import os
import requests
import subprocess
import re

context = ('dev.dcc.icgc.org.crt', 'dev.dcc.icgc.org.pem')

app = Flask(__name__)

build_user = 'dcc-jenkins'

token = ''
headers = {'Authorization': 'token %s' % token}

all_pr_url = 'https://api.github.com/repos/icgc-dcc/dcc-portal/pulls'
pr_url = all_pr_url + '/'


def get_slots():
    script_dir = os.path.dirname(__file__)
    with open(os.path.join(script_dir, './conf/slots.json')) as json_file:
        return json.load(json_file)['slots']


def write_slots(slots):
    script_dir = os.path.dirname(__file__)
    with open(os.path.join(script_dir, './conf/slots.json'), mode='w') as json_file:
        json_file.write(json.dumps({'slots': slots}))


def get_slot(slot_id):
    script_dir = os.path.dirname(__file__)
    with open(os.path.join(script_dir, './conf/slots.json')) as json_file:
        slots = json.load(json_file)['slots']
        for slot in slots:
            if slot['id'] == int(slot_id):
                return slot


def get_available_prs():
    r = requests.get(all_pr_url, headers=headers)
    return r.json()


def get_pr(pr):
    r = requests.get(pr_url + pr, headers=headers)
    pr_json = r.json()
    return pr_json


def get_existing_build(slot_id):
    slot = get_slot(slot_id)
    return {
        'pr': slot['pr'],
        'pr_title': slot['pr_title'],
        'pr_author': slot['pr_author'],
        'avatar_url': slot['avatar_url'],
        'build_number': slot['build_number'],
        'commit_id': slot['commit_id'],
        'branch': slot['branch']
    }


def save_slot(slot):
    slot_id = slot['id']
    slots = get_slots()
    slots[int(slot_id - 1)] = slot
    write_slots(slots)


def get_new_build(pr):
    pr_json = get_pr(pr)

    pr_title = pr_json['title']
    branch = pr_json['head']['ref']
    commit_id = pr_json['head']['sha']
    pr_author = pr_json['user']['login']
    avatar_url = pr_json['user']['avatar_url']

    status_json = requests.get(pr_json['statuses_url'], headers=headers).json()
    jenkins_json = [x for x in status_json if x['creator']['login'] == build_user][0]

    build_number = jenkins_json['target_url'].split('/')[-2]

    return {
        'pr': int(pr),
        'pr_title': pr_title,
        'pr_author': pr_author,
        'avatar_url': avatar_url,
        'branch': branch,
        'commit_id': commit_id,
        'build_number': build_number
    }


def deploy_by_build(slot_id, build_number):
    directory = get_slot(slot_id)['directory']
    installer = os.path.join(directory, 'bin', 'install')

    args = [installer, '-p', build_number]
    p = subprocess.Popen(args, stdout=subprocess.PIPE)
    output = p.stdout.read().decode("utf-8")

    return output


def get_status(slot):
    try:
        directory = slot['directory']
        exe = os.path.join(directory, 'bin', 'dcc-portal-server')

        args = [exe, 'status']
        p = subprocess.Popen(args, stdout=subprocess.PIPE)
        output = p.stdout.read().decode("utf-8")

        if re.match(r'DCC Portal is not running.', output):
            return 0
        elif re.match(r'DCC Portal is running:', output):
            return 1
        else:
            return -1
    except:
        return -1


def get_logs(slot):
    directory = slot['directory']
    logfile = os.path.join(directory, 'logs', 'dcc-portal-server.log')

    args = ['tail', '-n', '500', logfile]
    p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    output = p.stdout.read().decode("utf-8")
    output += p.stderr.read().decode("utf-8")

    return output


@app.route('/')
def home():
    slots = get_slots()

    for slot in slots:
        slot['status'] = get_status(slot)

    return render_template('home.html', slots=slots)


@app.route('/edit/<slot_id>')
def edit(slot_id):
    slot = get_slot(slot_id)
    prs = get_available_prs()
    return render_template('edit.html', slot=slot, prs=prs)


@app.route('/save/<slot_id>', methods=['POST'])
def save(slot_id):
    data = request.form

    updated_slot = {
        'id': int(slot_id),
        'name': data['name'],
        'description': data['description'],
        'directory': data['directory'],
        'url': data['url']
    }

    pr = data['prRadios']
    build_info = get_existing_build(slot_id) if pr == '0' else get_new_build(pr)

    slot = {**updated_slot, **build_info}
    save_slot(slot)

    output = 'No build output.'
    if pr != str(0):
        output = deploy_by_build(slot_id, slot['build_number'])

    return render_template('saved.html', output=output)


@app.route('/stop/<slot_id>')
def stop(slot_id):
    slot = get_slot(slot_id)
    directory = slot['directory']

    prog = os.path.join(directory, 'bin', 'dcc-portal-server')

    args = [prog, 'stop']
    p = subprocess.Popen(args, stdout=subprocess.PIPE)
    output = p.stdout.read().decode("utf-8")

    return render_template('stop.html', output=output)


@app.route('/start/<slot_id>')
def start(slot_id):
    slot = get_slot(slot_id)
    directory = slot['directory']

    prog = os.path.join(directory, 'bin', 'dcc-portal-server')

    args = [prog, 'start']
    p = subprocess.Popen(args, stdout=subprocess.PIPE)
    output = p.stdout.read().decode("utf-8")

    return render_template('start.html', output=output)


@app.route('/view/<slot_id>', methods=['GET'])
def view(slot_id):
    slot = get_slot(slot_id)
    return render_template('view.html', slot=slot)


@app.route('/log/<slot_id>', methods=['GET'])
def log(slot_id):
    slot = get_slot(slot_id)
    output = get_logs(slot)
    return render_template('log.html', output=output)


@app.route('/favicon.ico')
def favicon():
    return send_from_directory(os.path.join(app.root_path, 'static'),
                               'favicon.ico', mimetype='image/vnd.microsoft.icon')


if __name__ == '__main__':
    # app.run(host='0.0.0.0', port=443, debug=True, threaded=True, ssl_context=context)
    app.run(host='0.0.0.0', port=8443, debug=True)
