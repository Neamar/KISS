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

screenshots = sorted(glob.glob('./graphic_templates/phoneScreenshots/*.svg'))
default_screenshots = sorted(glob.glob('./metadata/android/en-US/images/phoneScreenshots/*.png'))


def get_last_change(file):
    """
    Returns the last time the file was modified on Git
    """
    try:
        out = subprocess.check_output(['git', 'log', '-1', r'--pretty=%ci', file]).strip()
    except subprocess.CalledProcessError:
        out = b"2020-01-01 00:00:00 +0200"
    if out.decode('ascii') == '':
        out = b"2020-01-01 00:00:00 +0200"
    return datetime.datetime.strptime(out.decode('ascii'), r"%Y-%m-%d %H:%M:%S %z")


def build_image(template_path, text_path, out_path, placeholder):
    if not os.path.exists(text_path):
        return False

    text_modified_date = get_last_change(text_path)
    template_modified_date = get_last_change(template_path)
    out_modified_date = get_last_change(out_path)

    if text_modified_date <= out_modified_date and template_modified_date <= out_modified_date:
        return False

    print("Generating %s for %s" % (out_path[out_path.rindex('/'):], locale))

    with open(text_path) as f:
        text = f.readline().strip()

    with open(template_path, 'r') as i:
        with open('/tmp/out.svg', 'w') as o:
            for l in i.readlines():
                l = l.replace(placeholder, text)
                o.write(l)

    subprocess.check_output(['inkscape', '--export-type=png', '--export-filename=/tmp/out.png', '--export-dpi=96', '/tmp/out.svg'])

    shutil.move("/tmp/out.png", os.getcwd() + '/' + out_path.replace('./', ''))
    return True


for locale in locales:
    if locale == 'en-US':
        continue

    os.makedirs('%s%s/images/phoneScreenshots' % (LOCALES_PREFIX, locale), exist_ok=True)

    ######
    # Feature graphic
    ######
    feature_graphic_template_path = './graphic_templates/featureGraphic.svg'
    feature_graphic_text_path = '%s%s/short_description.txt' % (LOCALES_PREFIX, locale)
    feature_graphic_out_path = '%s%s/images/featureGraphic.png' % (LOCALES_PREFIX, locale)
    build_image(feature_graphic_template_path, feature_graphic_text_path, feature_graphic_out_path, 't:featureGraphic.subtitle')

    ######
    # Screenshots
    # Only generate screenshots if all files are available, otherwise, use default.
    ######
    generated = []
    for screenshot_template_path in screenshots:
        screenshot_id = screenshot_template_path[screenshot_template_path.rindex('/')+1:-4]
        screenshot_text_path = '%s%s/screenshot_%s.txt' % (LOCALES_PREFIX, locale, screenshot_id)
        screenshot_out_path = '%s%s/images/phoneScreenshots/%s.png' % (LOCALES_PREFIX, locale, screenshot_id)

        if not os.path.exists(screenshot_text_path):
            # at least one file missing, abort.
            shutil.rmtree('%s%s/images/phoneScreenshots' % (LOCALES_PREFIX, locale))
            break

        build_image(screenshot_template_path, screenshot_text_path, screenshot_out_path, 't:screenshot.text')
        generated.append(screenshot_id)


    if len(generated) > 0:
        # add phone screenshots too
        for en_us_file_name in default_screenshots:
            screenshot_id = en_us_file_name[en_us_file_name.rindex('/')+1:-4]
            if screenshot_id not in generated:
                shutil.copy(en_us_file_name, en_us_file_name.replace('en-US', locale))
