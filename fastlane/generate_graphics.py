import glob
import json
import xml.etree.ElementTree as ET
import subprocess
import datetime
import pytz
import os
import shutil

LOCALES_PREFIX = './metadata/android/'
locales = [l.replace(LOCALES_PREFIX, '') for l in sorted(glob.glob(LOCALES_PREFIX + '*'))]

screenshots = ['./graphic_templates/phoneScreenshots/1.svg', './graphic_templates/phoneScreenshots/2.svg', './graphic_templates/phoneScreenshots/3.svg', './graphic_templates/phoneScreenshots/4.svg']
graphics = ['./graphic_templates/featureGraphic.svg']


def get_last_change(file):
    """
    Returns the last time the file was modified on Git
    """
    try:
        out = subprocess.check_output(['git', 'log', '-1', r'--pretty=%ci', file], stderr=subprocess.DEVNULL).strip()
    except subprocess.CalledProcessError:
        out = b"2020-01-01 00:00:00 +0200"
    return datetime.datetime.strptime(out.decode('ascii'), r"%Y-%m-%d %H:%M:%S %z")


for locale in locales:
    os.makedirs('%s%s/images/phoneScreenshots' % (LOCALES_PREFIX, locale), exist_ok=True)

    ######
    # Feature graphic
    ######
    feature_graphic_template_path = './graphic_templates/featureGraphic.svg'
    feature_graphic_text_path = '%s%s/short_description.txt' % (LOCALES_PREFIX, locale)
    feature_graphic_out_path = '%s%s/images/featureGraphic.png' % (LOCALES_PREFIX, locale)

    if not os.path.exists(feature_graphic_text_path):
        continue

    text_modified_date = get_last_change(feature_graphic_text_path)
    template_modified_date = get_last_change(feature_graphic_template_path)
    out_modified_date = get_last_change(feature_graphic_out_path)

    if text_modified_date <= out_modified_date and template_modified_date <= out_modified_date:
        continue

    print("Generating featureGraphic for %s" % locale)
    with open(feature_graphic_text_path) as f:
        feature_graphic_text = f.readline().strip()

    with open(feature_graphic_template_path, 'r') as i:
        with open('/tmp/out.svg', 'w') as o:
            for l in i.readlines():
                l = l.replace('t:featureGraphic.subtitle', feature_graphic_text)
                o.write(l)

    subprocess.check_output(['inkscape', '--export-type=png', '--export-filename=/tmp/out.png', '--export-dpi=96', '/tmp/out.svg'])

    shutil.move("/tmp/out.png", os.getcwd() + '/' + feature_graphic_out_path.replace('./', ''))
